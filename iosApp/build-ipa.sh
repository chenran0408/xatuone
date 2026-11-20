#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
output="${1:-$PWD/build}"
mkdir -p "$output"
output="$(cd "$output" && pwd)"
xcodegen generate
xcodebuild -quiet -project XatuOne.xcodeproj -scheme XatuOne -configuration Release \
  -destination 'generic/platform=iOS' -derivedDataPath "$output/device" \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO DEBUG_INFORMATION_FORMAT=dwarf-with-dsym build
products="$output/device/Build/Products/Release-iphoneos"
app="$products/XatuOne.app"
stage="$(mktemp -d)"
trap 'rm -rf "$stage"' EXIT
mkdir "$stage/Payload"
ditto "$app" "$stage/Payload/XatuOne.app"
app="$stage/Payload/XatuOne.app"
widget="$app/PlugIns/CourseWidget.appex"
check_bundle() {
  local bundle="$1" product="$2" identifier="$3" binary="$1/$2" commands
  test "$(xcrun lipo -archs "$binary")" = arm64
  xcrun vtool -show-build "$binary" | grep -E 'platform (IOS|2)$' >/dev/null
  test "$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$bundle/Info.plist")" = "$identifier"
  test "$(/usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' "$bundle/Info.plist")" = "$product"
  test "$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$bundle/Info.plist")" = 0.2.0
  test "$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$bundle/Info.plist")" = 2
  test "$(/usr/libexec/PlistBuddy -c 'Print :MinimumOSVersion' "$bundle/Info.plist")" = 16.0
  python3 -c 'import plistlib,sys; p=plistlib.load(open(sys.argv[1],"rb")); assert p["UIDeviceFamily"] == [1] and p["CFBundleSupportedPlatforms"] == ["iPhoneOS"]' "$bundle/Info.plist"
  if codesign -d "$bundle" 2>/dev/null; then codesign --remove-signature "$bundle"; fi
  # Strip only the packaged copy. Keep the matching dSYM for symbolication.
  local symbols="$products/$(basename "$bundle").dSYM"
  local uuid
  test -s "$symbols/Contents/Resources/DWARF/$product"
  uuid="$(xcrun dwarfdump --uuid "$symbols" | awk '{print $2}')"
  test -n "$uuid"
  xcrun strip -S -x "$binary"
  test "$(xcrun dwarfdump --uuid "$binary" | awk '{print $2}')" = "$uuid"
  commands="$(xcrun otool -l "$binary")"
  if grep -Eq '^[[:space:]]*cmd LC_CODE_SIGNATURE$' <<<"$commands" || codesign -d "$bundle" 2>/dev/null; then
    echo "Code signature remains in $product" >&2
    exit 1
  fi
}
check_bundle "$widget" CourseWidget com.xatuone.campus.widget
check_bundle "$app" XatuOne com.xatuone.campus
test "$(/usr/libexec/PlistBuddy -c 'Print :NSExtension:NSExtensionPointIdentifier' "$widget/Info.plist")" = com.apple.widgetkit-extension
test -z "$(find "$app" \( -name '*.mobileprovision' -o -name CodeResources \) -print)"
cmp "$app/myschool.js" ../shared/assets/offline_repo/schools/resources/XATU/myschool.js
version="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$app/Info.plist")"
ipa="$output/XATU-One-iPhone-$version-unsigned.ipa"
rm -f "$ipa"
(cd "$stage" && /usr/bin/zip -q -9 -r "$ipa" Payload)
unzip -tq "$ipa"
(cd "$output" && shasum -a 256 "$(basename "$ipa")" > SHA256SUMS)
