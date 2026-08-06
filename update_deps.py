with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

deps_to_add = """
  implementation("androidx.biometric:biometric:1.2.0-alpha05")
  implementation("com.google.code.gson:gson:2.10.1")
"""

content = content.replace('implementation(libs.androidx.room.runtime)', 'implementation(libs.androidx.room.runtime)' + deps_to_add)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)

