import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

System.load(Path("target/debug/libmpv_rs.so").absolutePathString())
object TestNatives {
    @JvmStatic
    external fun testN()

    @JvmStatic
    @Suppress("unused") // used by native code
    fun test(s: String) = println(s)
}

TestNatives.testN()
