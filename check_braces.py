lines = open(r'D:\source\HabitPulse\app\src\main\java\io\github\darrindeyoung791\habitpulse\ui\screens\RecordsScreen.kt', encoding='utf-8').readlines()
depth = 0
# RecordsScreenContent starts at line 158 (index 157)
# The function signature opens at line 163 with ) {
for i in range(157, 400):
    line = lines[i]
    opens = line.count('{')
    closes = line.count('}')
    new_depth = depth + opens - closes
    if opens != closes or i >= 378:
        print(f'Line {i+1:3d} | depth {depth} -> {new_depth} | opens={opens} closes={closes} | {line.rstrip()[:100]}')
    depth = new_depth
print(f'\nFinal depth at line 400: {depth} (should be 0 if balanced)')
