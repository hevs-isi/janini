import org.codehaus.commons.compiler.CompileException
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Test the WebServer class.
 */
class TestJaninoClasses {
    /**
     * Test simple math.
     */
    @Test
    fun testMath() {
        val classes = JaninoClasses().run("""
public class Question {
    public static void main(final String[] unused) {
        int a = 3;
        int b = 4;
        System.out.print(a + b);
    }
}
""")
        Assert.assertEquals(classes.output, "7")
        Assert.assertEquals(classes.compiler, "Janino")
    }

    /**
     * Test attempt to exit.
     */
    @Test
    fun testExit() {
        val classes = JaninoClasses().run("""
public class Question {
    public static void main(final String[] unused) {
        System.exit(-1);
    }
}
""")
        Assert.assertTrue(classes.compiled)
        Assert.assertEquals(classes.compiler, "Janino")
        Assert.assertFalse(classes.executed)
    }

    /**
     * Test wrong class name.
     */
    @Test
    fun testWrongClassName() {
        val classes = JaninoClasses()
        try {
            classes.run("""
public class Blah {
    public static void main(final String[] unused) {
        System.out.print("Broken");
    }
}
""")
        } catch (e : ClassNotFoundException) { }

        Assert.assertFalse(classes.compiled)
    }

    /**
     * Test wrong method name.
     */
    @Test
    fun testWrongMethodName() {
        val classes = JaninoClasses()
        try {
            classes.run("""
public class Question {
    public static void broken(final String[] unused) {
        System.out.print("Broken");
    }
}
""")
        } catch (e : NoSuchMethodException) { }

        Assert.assertFalse(classes.compiled)
    }

    /**
     * Test wrong method signature.
     */
    @Test
    fun testWrongMethodSignature() {
        val classes = JaninoClasses()
        try {
            classes.run("""
public class Question {
    public static void main() {
        System.out.print("Broken");
    }
}
""")
        } catch (e : NoSuchMethodException) { }

        Assert.assertFalse(classes.compiled)
    }

    /**
     * Verify the Janino doesn't support generics.
     */
    @Test
    fun testGenericsAreBrokenUsingJanino() {
        val classes = JaninoClasses("Janino")
        try {
            classes.run("""
import java.util.ArrayList;

public class Question {
    public static void main(final String[] unused) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("Geoffrey");
        String broken = list.get(0);
        System.out.print(broken);
    }
}
""")
        } catch (e: CompileException) { }

        Assert.assertFalse(classes.compiled)
    }

    /**
     * Verify that generics work using the JDK compiler.
     */
    @Test
    fun testGenericsWorkUsingJDK() {
        val classes = JaninoClasses("JDK").run("""
import java.util.ArrayList;

public class Question {
    public static void main(final String[] unused) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("Geoffrey");
        String working = list.get(0);
        System.out.print(working);
    }
}
""")

        Assert.assertEquals(classes.output, "Geoffrey")
        Assert.assertEquals(classes.compiler, "JDK")
    }

    /**
     * Verify that generics trigger the JDK.
     */
    @Test
    fun testGenericsTriggerTheJDK() {
        val classes = JaninoClasses().run("""
import java.util.ArrayList;

public class Question {
    public static void main(final String[] unused) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("Geoffrey");
        String working = list.get(0);
        System.out.print(working);
    }
}
""")

        Assert.assertEquals(classes.output, "Geoffrey")
        Assert.assertEquals(classes.compiler, "JDK")
    }

    /**
     * Verify that imports work properly.
     */
    @Test
    fun testImports() {
        val classes = JaninoClasses().run("""
import java.util.ArrayList;

public class Question {
    public static void main(final String[] unused) {
        System.out.print("Worked");
    }
}
""")
        Assert.assertEquals(classes.output, "Worked")
        Assert.assertEquals(classes.compiler, "Janino")
    }

    /**
     * Test multiple classes in same source.
     */
    @Test
    fun testMultipleClassesInSingleSource() {
        val classes = JaninoClasses().run("""
    public class Other {
        public String toString() {
            return "Working";
        }
    }
    public class Question {
        public static void main(final String[] unused) {
            Other other = new Other();
            System.out.print(other.toString());
        }
    }
    """)

        Assert.assertEquals(classes.output, "Working")
    }

    /**
     * Test multiple classes in same source in wrong order.
     */
    @Test
    fun testMultipleClassesInSingleSourceInWrongOrder() {
        val classes = JaninoClasses().run("""
    public class Question {
        public static void main(final String[] unused) {
            Other other = new Other();
            System.out.print(other.toString());
        }
    }
    public class Other {
        public String toString() {
            return "Working";
        }
    }
    """)

        Assert.assertEquals(classes.output, "Working")
    }

    /**
     * Test multiple classes in multiple sources.
     */
    @Test
    fun testMultipleClassesInMultipleSources() {
        val classes = JaninoClasses("Janino").run("""
public class Other {
    public String toString() {
        return "Working";
    }
}
""","""
public class Question {
    public static void main(final String[] unused) {
        Other other = new Other();
        System.out.print(other.toString());
    }
}
""")

        Assert.assertEquals(classes.output, "Working")
    }

    /**
     * Test multiple classes in multiple sources with imports.
     */
    @Test
    fun testMultipleClassesInMultipleSourcesWitImports() {
        val classes = JaninoClasses("Janino").run("""
import java.util.HashMap;
public class Other {
    public String toString() {
        HashMap hashMap = new HashMap();
        return "Working";
    }
}
""","""
import java.util.ArrayList;
public class Question {
    public static void main(final String[] unused) {
        ArrayList arrayList = new ArrayList();
        Other other = new Other();
        System.out.print(other.toString());
    }
}
""")

        Assert.assertEquals(classes.output, "Working")
    }

    /**
     * Test multiple classes in multiple sources in the wrong order.
     *
     * Currently we expect this to fail.
     */
    @Test
    fun testMultipleClassesInMultipleSourcesInWrongOrder() {
        val classes = JaninoClasses("Janino")
        try {
            classes.run("""
public class Question {
    public static void main(final String[] unused) {
        Other other = new Other();
        System.out.print(other.toString());
    }
}
""","""
    public class Other {
    public String toString() {
        return "Working";
    }
}
""")
        } catch (e : CompileException) {
            return
        }
        Assert.fail("Should fail")
    }
}
