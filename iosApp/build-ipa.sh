#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
output="${1:-$PWD/build}"
mkdir -p "$output"
output="$(cd "$output" && pwd)"
xcodegen generate
xcodebuild -quiet -project XatuOne.xcodeproj -scheme XatuOne -configuration Release \
  -destination 'generic/platform=iOS' -derivedDataPath "$output/device" \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO build
app="$output/device/Build/Products/Release-iphoneos/XatuOne.app"
stage="$(mktemp -d)"
trap 'rm -rf "$stage"' EXIT
mkdir "$stage/Payload"
ditto "$app" "$stage/Payload/XatuOne.app"
app="$stage/Payload/XatuOne.app"
test "$(xcrun lipo -archs "$app/XatuOne")" = arm64
xcrun vtool -show-build "$app/XatuOne" | grep -Eq 'platform (IOS|2)$'
test "$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app/Info.plist")" = com.xatuone.campus
test ! -e "$app/embedded.mobileprovision"
test -s "$app/myschool.js"
if codesign -d "$app" 2>/dev/null; then codesign --remove-signature "$app"; fi
version="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$app/Info.plist")"
ipa="$output/XATU-One-iPhone-$version-unsigned.ipa"
rm -f "$ipa"
(cd "$stage" && /usr/bin/zip -qr "$ipa" Payload)
unzip -tq "$ipa"
(cd "$output" && shasum -a 256 "$(basename "$ipa")" > SHA256SUMS)
