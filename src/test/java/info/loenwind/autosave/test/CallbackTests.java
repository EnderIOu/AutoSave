package info.loenwind.autosave.test;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import info.loenwind.autosave.Reader;
import info.loenwind.autosave.Writer;
import info.loenwind.autosave.annotations.AfterRead;
import info.loenwind.autosave.annotations.Factory;
import info.loenwind.autosave.annotations.Storable;
import info.loenwind.autosave.annotations.Store;

public class CallbackTests {

    @Storable
    private static abstract class Base {

        @Store
        protected List<String> strings;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Base other)) return false;
            return strings.equals(other.strings);
        }
    }

    @Storable
    private static class WithPublicConstructor extends Base {

        private static final List<String> VALS = Lists.newArrayList("Public", "constructor", "as", "a", "factory");

        public WithPublicConstructor() {
            this.strings = VALS;
        }
    }

    @Storable
    private static class WithFactoryConstructor extends Base {

        private static final List<String> VALS = Lists.newArrayList("Private", "constructor", "marked", "as",
                "factory");

        @Factory
        private WithFactoryConstructor() {
            this.strings = VALS;
        }
    }

    @Storable
    private static class WithFactoryMethod extends Base {

        private static final List<String> VALS = Lists.newArrayList("Static", "method", "marked", "as", "factory");

        @Factory
        static WithFactoryMethod create() {
            WithFactoryMethod ret = new WithFactoryMethod();
            ret.strings = VALS;
            return ret;
        }
    }

    private static class Holder {

        public @Store WithPublicConstructor publicCtor;
        public @Store WithFactoryConstructor privateCtor;
        public @Store WithFactoryMethod factoryMethod;

        public void fill() {
            publicCtor = new WithPublicConstructor();
            privateCtor = new WithFactoryConstructor();
            factoryMethod = WithFactoryMethod.create();
        }
    }

    private static final @Nonnull Holder before = new Holder();
    private static final @Nonnull Holder after = new Holder();

    @BeforeAll
    public static void setup() {
        // Log.enableExtremelyDetailedNBTActivity("AutoStoreTests", true);

        before.fill();

        NBTTagCompound tag = new NBTTagCompound();
        Writer.write(tag, before);
        Reader.read(tag, after);
    }

    @Test
    public void testPublicConstructor() {
        Assertions.assertEquals(before.publicCtor, after.publicCtor);
    }

    @Test
    public void testFactoryConstructor() {
        Assertions.assertEquals(before.privateCtor, after.privateCtor);
    }

    @Test
    public void testFactoryMethod() {
        Assertions.assertEquals(before.factoryMethod, after.factoryMethod);
    }

    @Test
    public void testAfterRead() {
        class CallbackTest {

            boolean callbackInvoked;

            @AfterRead
            public void onRead() {
                callbackInvoked = true;
            }
        }

        CallbackTest test = new CallbackTest();
        Reader.read(new NBTTagCompound(), test);
        Assertions.assertTrue(test.callbackInvoked);
    }
}
