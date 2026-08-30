import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    out_lines = []
    in_main_content = False
    brace_level = 0
    lazy_col_level = -1
    inner_col_level = -1
    
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Replace Outer Column with LazyColumn
        if "modifier = Modifier" in line and ".fillMaxSize()" in lines[i+1] and ".padding(innerPadding)" in lines[i+2] and ".verticalScroll" in lines[i+3]:
            # It's the outer Column
            out_lines.append(line.replace("Column", "LazyColumn"))
            out_lines.append(lines[i+1])
            out_lines.append(lines[i+2])
            out_lines.append(lines[i+4]) # Skip verticalScroll
            out_lines.append(lines[i+5]) # {
            i += 5
            in_main_content = True
            lazy_col_level = brace_level
            brace_level += 1
            i += 1
            continue
            
        if in_main_content and "Column(" in line and "fillMaxWidth" in lines[i+1] and "widthIn(max = 600" in lines[i+2]:
            # This is the inner column. We will keep it but as an item wrapper?
            # No, if we keep it, it wraps everything. We must NOT emit this inner Column.
            # Instead, we will wrap sections in item { Column(...) { ... } }
            inner_col_level = brace_level
            brace_level += 1
            # Skip this line and the next 4 lines
            i += 4 # up to horizontalAlignment
            # Note: the { is on the next line or same line?
            if "{" in lines[i]:
                 pass
            elif "{" in lines[i+1]:
                 i += 1
            i += 1
            continue

        # Count braces
        open_braces = line.count('{')
        close_braces = line.count('}')
        
        if in_main_content and brace_level == inner_col_level + 1:
            # We are at the top level of the inner column
            # Whenever we see a top-level component, we wrap it in item { Column(...) {
            pass
            
        brace_level += open_braces - close_braces
        
        out_lines.append(line)
        i += 1
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(out_lines)

process_file('app/src/main/java/com/example/ui/screens/SettingsScreen.kt')
