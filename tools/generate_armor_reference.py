# -*- coding: utf-8 -*-
# Генератор справочника брони WRB (Warborn Renewed) + SBW.
# Статы материалов и кусков извлекаются из bytecode через javap -v (JDK 17).
# Запуск: py -3 tools/generate_armor_reference.py
import zipfile, json, os, re, subprocess, tempfile, shutil

SERVER = r"C:\Users\maska\OneDrive\Desktop\PWP-Server"
BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WRB_JAR = os.path.join(SERVER, "mods", "WRB-Armor-0.4.1.jar")
SBW_JAR = os.path.join(SERVER, "mods", "superbwarfare-0.8.9-final-mc1.20.1-6effe4385-all.jar")
JAVAP = r"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javap.exe"
OUT = os.path.join(BASE, "docs", "ARMOR_REFERENCE.md")

# --- материалы (выведены декомпиляцией ModArmorMaterials.class) ---
MATERIALS = {
    "LEATHER":     dict(dur=5,  prot=[0, 0, 0, 0], enchant=0,  toughness=0.0, kb=0.00, slot_dur=[13, 15, 16, 11]),
    "KEVLAR":      dict(dur=20, prot=[1, 3, 5, 2], enchant=15, toughness=0.5, kb=0.00, slot_dur=None),
    "CERAMIC":     dict(dur=30, prot=[2, 5, 7, 3], enchant=12, toughness=2.5, kb=0.05, slot_dur=None),
    "AR500_STEEL": dict(dur=40, prot=[2, 4, 6, 2], enchant=10, toughness=2.0, kb=0.10, slot_dur=None),
    "UHMWPE":      dict(dur=45, prot=[3, 6, 8, 3], enchant=18, toughness=3.0, kb=0.08, slot_dur=None),
    "COMPOSITE":   dict(dur=55, prot=[3, 7, 9, 4], enchant=15, toughness=4.0, kb=0.15, slot_dur=None),
    "TITANIUM":    dict(dur=60, prot=[4, 7, 9, 4], enchant=20, toughness=4.5, kb=0.20, slot_dur=None),
}


def run_javap(classname, jar, tmpdir):
    pkg_path = classname.replace(".", "/")
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if n.endswith(".class") and n.startswith(pkg_path):
                out = os.path.join(tmpdir, n.replace("/", os.sep))
                os.makedirs(os.path.dirname(out), exist_ok=True)
                with open(out, "wb") as f:
                    f.write(z.read(n))
    r = subprocess.run([JAVAP, "-p", "-c", "-v", "-classpath", tmpdir, classname],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout


def parse_sets(javap_out):
    """Возвращает {имя_куска: {material, bullet, prot, speed}}"""
    lines = javap_out.splitlines()

    pool = {}  # #N -> bootstrap index (для InvokeDynamic)
    for l in lines:
        m = re.match(r"#(\d+) = InvokeDynamic\s+#(\d+):#\d+", l.strip())
        if m:
            pool[int(m.group(1))] = int(m.group(2))

    bootstrap = {}  # bootstrap index -> lambda-имя impl-метода
    bms = [i for i, l in enumerate(lines) if l.strip() == "BootstrapMethods:"]
    if bms:
        i = bms[0] + 1
        cur = None
        while i < len(lines):
            m = re.match(r"(\d+): #\d+ REF_invokeStatic", lines[i].strip())
            if m:
                cur = int(m.group(1))
            elif cur is not None and "REF_invokeStatic" in lines[i]:
                mm = re.search(r"REF_invokeStatic [\w/]+\.(lambda\$[\w$]+):", lines[i])
                if mm:
                    bootstrap[cur] = mm.group(1)
                    cur = None  # implMethod найден
            i += 1

    methods = {}  # lambda-имя -> строки кода
    cur_method = None
    for l in lines:
        m = re.match(r"\s*private static [\w.$<>/, ]*?(lambda\$[\w$]+)\s*\(", l)
        if m:
            cur_method = m.group(1)
            methods[cur_method] = []
        elif re.match(r"\s*(public|private|protected|static|final)\s", l) and not re.match(r"\s+\d+:", l):
            cur_method = None
        elif cur_method is not None and re.match(r"\s+\d+:", l):
            methods[cur_method].append(l.strip())

    # материал из лямбд, где getstatic ModArmorMaterials
    material_of = {}
    for name, body in methods.items():
        for l in body:
            mm = re.match(r"\d+: getstatic\s+#\d+\s+// Field ru/liko/warbornrenewed/registry/ModArmorMaterials\.(\w+):", l)
            if mm:
                material_of[name] = mm.group(1)
                break

    def prev_value(body, idx):
        j = idx - 1
        while j >= 0:
            l = body[j]
            m = re.match(r"\d+: (ldc2_w\s+#\d+\s+// double ([-\d.]+)d|ldc\s+#\d+\s+// (?:int|float) ([\d.]+)|dconst_(\d)|fconst_(\d)|bipush\s+(\d+)|sipush\s+(\d+)|iconst_(\d))", l)
            if m:
                for g in range(2, 9):
                    if m.group(g) is not None:
                        return float(m.group(g)) if "." in m.group(g) else int(m.group(g))
            if re.search(r"invoke(virtual|static|dynamic|interface)", l):
                return None
            j -= 1
        return None

    pieces = {}
    for name, body in methods.items():
        cur = None
        for idx, l in enumerate(body):
            nm = re.match(r"\d+: ldc(?:_w)?\s+#\d+\s+// String (.+)", l)
            if nm and idx + 1 < len(body) and "registryName" in body[idx + 1]:
                cur = {"material": None, "bullet": None, "blast": None, "prot": None, "speed": None}
                pieces[nm.group(1)] = cur
                continue
            if cur is None:
                continue
            ind = re.match(r"\d+: invokedynamic\s+#(\d+)", l)
            if ind and idx + 1 < len(body) and "material:" in body[idx + 1]:
                bs = pool.get(int(ind.group(1)))
                if bs is not None:
                    cur["material"] = material_of.get(bootstrap.get(bs))
            if "bulletResistance:" in l:
                cur["bullet"] = prev_value(body, idx)
            if "blastResistance:" in l:
                cur["blast"] = prev_value(body, idx)
            if "protectionClass:" in l:
                cur["prot"] = prev_value(body, idx)
            if "movementSpeed:" in l:
                cur["speed"] = prev_value(body, idx)
    return pieces


def mat_row(mat):
    d = MATERIALS[mat]
    return f"| {mat} | {d['dur']} | {' / '.join(str(x) for x in d['prot'])} | {d['enchant']} | {d['toughness']} | {d['kb']} |"


def main():
    tmp = tempfile.mkdtemp(prefix="wrb_")
    try:
        sets_txt = run_javap("ru.liko.warbornrenewed.setup.WarbornArmorSets", WRB_JAR, tmp)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    pieces = parse_sets(sets_txt)

    items = []
    with zipfile.ZipFile(WRB_JAR) as z:
        for n in z.namelist():
            if n.startswith("assets/warbornrenewed/textures/item/") and n.endswith(".png"):
                items.append(os.path.basename(n)[:-4])
        en = json.loads(z.read("assets/warbornrenewed/lang/en_us.json"))
        ru = json.loads(z.read("assets/warbornrenewed/lang/ru_ru.json"))
    items.sort()

    md = ["# Справочник брони: Warborn Renewed (WRB) + SuperbWarfare", ""]
    md.append("> WRB 0.4.1 (`WRB-Armor-0.4.1.jar`). Статы извлечены декомпиляцией bytecode (javap).")
    md.append("")
    md.append("## Материалы WRB (ModArmorMaterials)")
    md.append("")
    md.append("| Материал | Прочность × | Защита (boots/legs/chest/helm) | Чары | Твёрдость | Отдача-резист |")
    md.append("|---|---|---|---|---|---|")
    for m in MATERIALS:
        md.append(mat_row(m))
    md.append("")
    md.append("Базовые прочности слотов: boots 13 / legs 15 / chest 16 / helm 11; итоговая прочность = база × множитель материала.")
    md.append("")
    md.append("## Куски брони WRB (WarbornArmorSets.class)")
    md.append("")
    md.append("| Кусок | Материал | bulletResistance | blastResistance | protectionClass | movementSpeed |")
    md.append("|---|---|---|---|---|---|")
    for name in sorted(pieces):
        p = pieces[name]
        md.append(f"| `{name}` | {p['material'] or '?'} | {p['bullet'] if p['bullet'] is not None else '?'} | {p['blast'] if p['blast'] is not None else '?'} | {p['prot'] if p['prot'] is not None else '?'} | {p['speed'] if p['speed'] is not None else '?'} |")
    md.append("")
    md.append("## Предметы WRB (registry id = `warbornrenewed:<id>`)")
    md.append("")
    md.append("| ID | Название (EN) | Название (RU) |")
    md.append("|---|---|---|")
    for iid in items:
        md.append(f"| `{iid}` | {en.get('item.warbornrenewed.' + iid, '?')} | {ru.get('item.warbornrenewed.' + iid, '?')} |")
    md.append("")
    md.append("## Броня SuperbWarfare")
    md.append("")
    md.append("| ID | Название |")
    md.append("|---|---|")
    sbw_items = ["ru_helmet_6b47", "ru_chest_6b43", "us_helmet_pasgt", "us_chest_iotv", "ge_helmet_m_35"]
    with zipfile.ZipFile(SBW_JAR) as z:
        sbw_en = json.loads(z.read("assets/superbwarfare/lang/en_us.json"))
    for iid in sbw_items:
        md.append(f"| `superbwarfare:{iid}` | {sbw_en.get('item.superbwarfare.' + iid, '?')} |")
    md.append("")

    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(md))
    print(f"OK {OUT} | кусков: {len(pieces)}, предметов: {len(items)}")


if __name__ == "__main__":
    main()
