-injars target/spyhunts-2.0.0.jar
-outjars target/spyhunts-2.0.0-obf.jar

-libraryjars "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\lib\modules"(!module-info.class)
-libraryjars "C:\Users\vikas\.m2\repository\io\papermc\paper\paper-api\1.21.1-R0.1-SNAPSHOT\paper-api-1.21.1-R0.1-20250328.161643-128.jar"(!META-INF/versions/**)
-libraryjars "C:\Users\vikas\.m2\repository\com\spygamingog\spycore\1.1.2\spycore-1.1.2.jar"

-dontshrink
-dontoptimize
-dontusemixedcaseclassnames
-keeppackagenames
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
-dontwarn

-keep public class com.spygamingog.spyhunts.SpyHuntsPlugin {
    public *;
}

-keepclassmembers class * {
    @org.bukkit.event.EventHandler *;
}

-keep public class * implements org.bukkit.command.CommandExecutor {
    public *;
}

-keep public class * implements org.bukkit.command.TabCompleter {
    public *;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
