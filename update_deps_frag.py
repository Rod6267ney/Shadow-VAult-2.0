with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

deps_to_add = """
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("androidx.fragment:fragment-ktx:1.6.2")
"""

content = content.replace('implementation("androidx.biometric:biometric:1.2.0-alpha05")', 'implementation("androidx.biometric:biometric:1.2.0-alpha05")' + deps_to_add)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)

