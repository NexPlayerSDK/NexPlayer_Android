# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

#To maintain custom components names that are used on layouts XML:
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
}
-keep public class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keep public class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Maintain java native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Maintain enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep the R
-keepclassmembers class **.R$* {
    public static <fields>;
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}


-dontwarn android.support.v4.app.**
-dontwarn com.nexstreaming.nexplayerengine.**

-keep class com.nexstreaming.nexplayerengine.NexALFactory{*;}
-keep class com.nexstreaming.nexplayerengine.NexALFactory$ICodecDownListener{*;}
-keep class com.nexstreaming.nexplayerengine.NexALFactory$NexALFactoryErrorCode{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$IListener{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$NexSDKType{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$NexProperty{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$PROGRAM_TIME{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$NexErrorCode{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$NexUniqueIDVer{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$IReleaseListener{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$NexErrorCategory{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$IVideoRendererListener{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$NexRTStreamInformation{*;}
-keep class com.nexstreaming.nexplayerengine.NexPlayer$IDynamicThumbnailListener{*;}
-keep class com.nexstreaming.nexplayerengine.NexCodecInformation{*;}
-keep class com.nexstreaming.nexplayerengine.NexContentInformation{*;}
-keep class com.nexstreaming.nexplayerengine.NexID3TagInformation{*;}
-keep class com.nexstreaming.nexplayerengine.NexID3TagPicture{*;}
-keep class com.nexstreaming.nexplayerengine.NexID3TagText{*;}
-keep class com.nexstreaming.nexplayerengine.NexPictureTimingInfo{*;}
-keep class com.nexstreaming.nexplayerengine.NexStreamInformation{*;}
-keep class com.nexstreaming.nexplayerengine.NexTrackInformation{*;}
-keep class com.nexstreaming.nexplayerengine.NexCaptionRenderer{*;}
-keep class com.nexstreaming.nexplayerengine.NexCaptionRendererForTimedText{*;}
-keep class com.nexstreaming.nexplayerengine.NexCaptionRendererForTimedText$CaptionData{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$CaptionColor{*;}
-keep class com.nexstreaming.nexplayerengine.NexCustomAttribInformation{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTMLRenderingData{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTMLRenderingData$TTMLNodeData{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTML_LengthType{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTML_DisplayAlign{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTML_Fontstyle{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTML_TextAlign{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTML_UnicodeBIDI{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTML_WritingMode{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTML_StyleLength{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TTML_TextOutlineStyleLength{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$WebVTTRenderingData{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$WebVTTRenderingData$WebVTTNodeData{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$WebVTT_TextAlign{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$WebVTT_WritingDirection{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TextStyleEntry{*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$TextKaraokeEntry {*;}
-keep class com.nexstreaming.nexplayerengine.NexClosedCaption$CaptionMode{*;}
-keep class com.nexstreaming.nexplayerengine.NexNetAddrTable{*;}
-keep class com.nexstreaming.nexplayerengine.NexNetAddrTable$NetAddrTableInfo{*;}
-keep class com.nexstreaming.nexplayerengine.NexClient{*;}
-keep class com.nexstreaming.nexplayerengine.NexWVDRM{*;}
-keep class com.nexstreaming.nexplayerengine.NexSessionData{*;}
-keep class com.nexstreaming.nexplayerengine.NexHLSAES128DRMManager{*;}
-keep class com.nexstreaming.nexplayerengine.NexDateRangeData{*;}
-keep class com.nexstreaming.nexplayerengine.NexEmsgData{*;}
-keep class com.nexstreaming.nexplayerengine.NexVSyncSampler{*;}


