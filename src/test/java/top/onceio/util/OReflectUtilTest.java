package top.onceio.util;

import java.lang.reflect.Type;

import org.junit.Assert;
import org.junit.Test;

import top.onceio.core.db.model.BaseMeta;
import top.onceio.core.db.model.BaseModel;
import top.onceio.core.util.OReflectUtil;

public class OReflectUtilTest {
    class Zero {

    }

    class A<T1> extends Zero {
        T1 id;
    }

    class B extends A<Long> {

    }

    class C<E, ID2> extends A<ID2> {
        E e;
    }

    class D extends C<Long, String> {

    }

    @Test
    public void searchGenType() throws NoSuchFieldException, SecurityException {
        A<Integer> a = new A<>();
        Type fieldType = a.getClass().getDeclaredField("id").getGenericType();
        Assert.assertEquals(String.class.getTypeName(), OReflectUtil.searchGenType(A.class, D.class, fieldType).getTypeName());
    }


}
