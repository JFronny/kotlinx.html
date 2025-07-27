import re

def fix_html5_table(input_path, output_path):
    with open(input_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Adjust tags content
    content = re.sub(r'(<tr><th(?:[^>]*)>\s?<code(?:[^>]*)>(?:[^<]+)\s*<\/code>(?:\s*<\/td>)?\s*)', r'\1</th>', content, flags=re.DOTALL)
    content = re.sub(r'<a href=([^"][^\s>]+)', r'<a href="\1"', content)
    content = re.sub(r'(<tr><th><code(?:[^>]*)><a href="(?:[^"]+)">(?:[^<]+)(?:(?!<tr>).)*<\/code>)(\s*<td>(?:<a href="(?:[^"]+)"[^>]*>)?(?:[^<]+)(?:(?!<tr>).)*(?:<\/td>)?)', r'\1</th>\2', content, flags=re.DOTALL)

    # content = re.sub(r'(<td>(?:(?!</td>|</th>|<th>).)*)</th>', r'\1', content, flags=re.DOTALL)

    # Adjust attribute content
    content = re.sub(r'(<tr><th(?:[^>]*)>\s?<code(?:[^>]*)>(?:[^<]+)\s*<\/code>(?:\s*<\/td>)?\s*<\/th>\s*<td>(?:[^;\n]*(?:;\s*[^;\n]*)*)\s*)<td>', r'\1</td><td>', content, flags=re.DOTALL)
    content = re.sub(r'(<tr><th(?:[^>]*)>\s?<code(?:[^>]*)>(?:[^<]+)\s*<\/code>(?:\s*<\/td>)?\s*<\/th>\s*<td>(?:[^;\n]*(?:;\s*[^;\n]*)*)(?:\s*<\/td>)\s*<td>\s*(?:(?!<td>).)*[\s\n]*)<td>', r'\1</td><td>', content, flags=re.DOTALL)

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    fix_html5_table("html5_new.html", "html5.html")
