# release 已开 R8（minify + shrinkResources）。这里只写「反射/动态装配」这一类必须留的规则，
# 不写包级 blanket keep——上一版 `-keep class com.wotagei.cam.media.** { *; }` 把相册、媒体库、
# Room 包装类整个排除在 shrinking 之外，等于白开 R8。
# 枚举的 values()/valueOf() 由 R8 内建处理，不需要 keep（参数恢复走的是 name 字符串）。

# Room：@Database 实现类由 Room 的工厂按生成的 `<Name>_Impl` 装配，实体注解运行期要读
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Media3：可选解码扩展（ffmpeg/av1/libvpx）按类名反射探测，本工程未打包它们，只压掉缺类告警
-dontwarn androidx.media3.decoder.**

# lifecycle-runtime-compose 2.8.x + Compose UI 1.6.x:
# LocalLifecycleOwner melakukan lookup refleksi ke AndroidCompositionLocals_androidKt.getLocalLifecycleOwner().
# Versi 2.8.2 punya typo consumer rule (`ProvidableCompositionLocal[]`) sehingga R8 tetap meng-obfuscate method ini.
-keep public class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt {
    public static androidx.compose.runtime.ProvidableCompositionLocal getLocalLifecycleOwner();
}
