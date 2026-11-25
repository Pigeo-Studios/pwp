"""Проверка reobf: в jar модов PWP миксины на ванилу должны ссылаться на SRG-имена (m_NNN_/f_NNN_),
а не на mojmap. Запуск после сборки: py -3 tools/check_reobf.py  (код выхода 1 — что-то не так).

Признаки mojmap-сборки (инцидент 16.11.2026): в WindowIconMixin нет m_91268_ (getWindow),
в jar с миксинами нет рефмапа или атрибута MixinConfigs.
"""
import glob
import re
import sys
import zipfile

sys.stdout.reconfigure(encoding="utf-8")

SRG = re.compile(rb"[mf]_\d+_")
bad = 0
for jar in sorted(glob.glob("*/build/libs/*.jar")):
    if "core-service" in jar or jar.endswith("-sources.jar"):
        continue
    with zipfile.ZipFile(jar) as z:
        names = z.namelist()
        manifest = z.read("META-INF/MANIFEST.MF").decode("utf-8", "replace") if "META-INF/MANIFEST.MF" in names else ""
        mixins = [n for n in names if "/mixin/" in n and n.endswith(".class")]
        srg = sum(1 for n in mixins if SRG.search(z.read(n)))
        refmaps = [n for n in names if n.endswith(".refmap.json")]
        mc = re.search(r"MixinConfigs: ?([^\r\n]+)", manifest)
        problems = []
        if mixins and not refmaps:
            problems.append("нет рефмапа")
        if mixins and not mc:
            problems.append("нет MixinConfigs")
        for n in names:
            if n.endswith("WindowIconMixin.class") and b"m_91268_" not in z.read(n):
                problems.append("WindowIconMixin без m_91268_ (mojmap!)")
        srg_ref = sum(len(SRG.findall(z.read(n))) for n in refmaps)
        if mixins and srg == 0 and srg_ref == 0:
            problems.append("ни одного SRG-имени в миксинах")
        status = "FAIL " + "; ".join(problems) if problems else "OK"
        bad += bool(problems)
        print(f"{status:6} {jar}: mixins={len(mixins)} srg={srg} srg_in_refmap={srg_ref if mixins else 0} refmaps={refmaps} configs={mc.group(1) if mc else '-'}")
sys.exit(1 if bad else 0)
