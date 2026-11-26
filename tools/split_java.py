"""Механический перенос статических методов Java-класса в новый класс того же пакета (без изменения тел).

Используется для разбора больших классов (этап 13 ROADMAP). Что делает:
  * вырезает методы по именам (с javadoc/аннотациями/комментариями прямо над ними) из исходного класса;
  * создаёт новый класс в том же пакете с теми же import'ами + `import static <исходный>.*;`,
    с аннотацией @EventBusSubscriber исходного (если есть);
  * в исходном классе `private static` → package-private (чтобы перенесённые методы видели помощников)
    и добавляет `import static <новый>.*;` (чтобы оставшиеся методы видели перенесённые).
Тела методов не меняются ни на символ — поведение то же. Проверка — компиляция.

  py -3 tools/split_java.py <Исходный.java> <НовыйКласс> "<javadoc одной строкой>" method1 method2 ...
"""
import re
import sys
from pathlib import Path


def find_method(src, name):
    """(start, end) метода name: начало — первая строка аннотаций/комментариев над сигнатурой."""
    sig = re.compile(r'^    (?:public |private |protected )?static [^\n;=]*?\b' + re.escape(name) + r'\(', re.M)
    m = sig.search(src)
    if not m:
        raise SystemExit(f"метод {name} не найден")
    # тело: от первой '{' после сигнатуры до парной '}' (строки/символы учитываются)
    i = src.index('{', m.end())
    depth, j, n = 0, i, len(src)
    while j < n:
        c = src[j]
        if c == '"':
            if src.startswith('"""', j):
                j = src.index('"""', j + 3) + 3
                continue
            j += 1
            while src[j] != '"':
                j += 2 if src[j] == '\\' else 1
        elif c == "'":
            j += 1
            while src[j] != "'":
                j += 2 if src[j] == '\\' else 1
        elif src.startswith('//', j):
            j = src.index('\n', j)
            continue
        elif src.startswith('/*', j):
            j = src.index('*/', j) + 2
            continue
        elif c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                end = j + 1
                break
        j += 1
    # захватить перевод строки после '}'
    if src[end:end + 1] == '\n':
        end += 1
    # начало: подняться над аннотациями/комментариями/javadoc
    start = src.rfind('\n', 0, m.start()) + 1
    lines_before = src[:start].split('\n')
    k = len(lines_before) - 1  # последний элемент — пустая строка после \n
    while k > 0:
        line = lines_before[k - 1].strip()
        if line.startswith('@') or line.startswith('//') or line.startswith('*') or line.startswith('/**') \
                or line.endswith('*/'):
            k -= 1
        else:
            break
    start = len('\n'.join(lines_before[:k])) + (1 if k > 0 else 0)
    return start, end


def main():
    path, new_name, doc, methods = Path(sys.argv[1]), sys.argv[2], sys.argv[3], sys.argv[4:]
    src = path.read_text(encoding='utf-8')
    pkg = re.search(r'^package ([\w.]+);', src, re.M).group(1)
    old_name = path.stem
    imports = re.findall(r'^import [^\n]+;$', src, re.M)
    subscriber = re.search(r'^@(?:Mod\.)?EventBusSubscriber\([^\n]*\)$', src, re.M)

    pieces = []
    for name in methods:
        s, e = find_method(src, name)
        pieces.append(src[s:e].rstrip('\n'))
        src = src[:s] + src[e:]
    src = re.sub(r'\n{3,}', '\n\n', src)

    # помощники исходного класса — package-private
    src = re.sub(r'^    private static ', '    static ', src, flags=re.M)
    own_static = f'import static {pkg}.{new_name}.*;'
    if own_static not in src:
        src = src.replace(f'package {pkg};\n', f'package {pkg};\n\n{own_static}\n', 1)
    path.write_text(src, encoding='utf-8', newline='\n')

    body = '\n\n'.join(pieces)
    head = [f'package {pkg};', '', f'import static {pkg}.{old_name}.*;', '']
    head += [i for i in imports if not i.startswith('import static')]
    head += [i for i in imports if i.startswith('import static') and old_name not in i and new_name not in i]
    out = '\n'.join(head) + '\n\n'
    out += '/**\n * ' + doc + '\n *\n * <p>Вынесено из {@link ' + old_name + '} без изменения кода (этап 13 ROADMAP).\n */\n'
    if subscriber:
        out += subscriber.group(0) + '\n'
    out += f'public final class {new_name} {{\n    private {new_name}() {{}}\n\n' + body + '\n}\n'
    (path.parent / f'{new_name}.java').write_text(out, encoding='utf-8', newline='\n')
    print(f'{new_name}: перенесено {len(pieces)} методов; {old_name}: {src.count(chr(10))} строк')


if __name__ == '__main__':
    main()
