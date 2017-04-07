package edu.tum.uc.jvm.checker;
/**
 * Created by xtarx on 23/02/2017.
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.objectweb.asm.Opcodes;

/**
 * 			if (!isConstructor && !isStatic) {
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				mv.visitVarInsn(Opcodes.ALOAD, paramArrayIndex);
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, CHECKERCLASS, "parseObjectBool", "(Ljava/lang/Object;[Ljava/lang/Object;)Z", false);
				mv.visitInsn(Opcodes.POP);
//				mv.visitInsn(Opcodes.ICONST_0);
//				mv.visitMethodInsn(Opcodes.INVOKESTATIC, System.class.getName().replace(".", "/"), "exit", "(I)V", false);
			}
 *
 */
public class RequestCheck {

    static HashMap hm = new HashMap();

    public static void parse_field(String str) {
        try {
            str = str.substring(0, str.length() - 1);
            String[] entries = str.split("##");


            for (String entry : entries) {
                String[] parts = entry.split("!&!");
                hm.put(parts[0], parts[1]);
            }
        } catch (Exception e) {

        }

    }

    public static String hashmap() {

        String str="";
        Set set = hm.entrySet();
        Iterator i = set.iterator();
        // Display elements
        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            str+=(me.getKey() + ": ");
            str+=(me.getValue());
            str+="/n";
        }
        return str;

    }


    public static boolean parseObjectBool(Object o, Object[] fieldName) {
    	System.out.println("RequestCheck.parse "+o);
        if (o instanceof HttpServletRequest) {
        	System.out.println("RequestCheck.parse: Instance of HttpServletRequest");
        	
            HttpServletRequest request = (HttpServletRequest) o;
            String protected_fields = request.getParameter("uc_protected_fields");

            System.out.println("RequestCheck.parse: protected field "+protected_fields);
            if (!"".equals(protected_fields) && protected_fields != null) {
                //if field doesnt excist in protected fields, remove it from hashmap
                if (!protected_fields.contains((String)fieldName[0])) {
                    hm.remove((String)fieldName[0]);
                }
                parse_field(protected_fields);
                boolean _return = containsField((String)fieldName[0]);
                System.out.println("RequestCheck.parse.containsField: "+_return+" , FN: "+fieldName[0]);
            }
        }
        return false;
    }



    public static String parseObject(Object o,Object[] fieldName) {

        if (o instanceof HttpServletRequest) {

            HttpServletRequest request = (HttpServletRequest) o;
            String protected_fields = request.getParameter("uc_protected_fields");
            if (protected_fields != null) {
                parse_field(protected_fields);
                return containsFieldWithPolicy((String)fieldName[0]);
            }
        }
        return null;
    }



    public static boolean containsField(String key) {
        if (hm.containsKey(key))
            return true;

        return false;
    }

    public static String containsFieldWithPolicy(String key) {

        if (hm.containsKey(key))
            return hm.get(key).toString();

        return null;

    }


    public static void main(String[] args) {

        String protected_fields = "first_name!&!policy1##last_name!&!policy2##age!&!policy3#";


        parseObjectBool(protected_fields, null);

        System.out.println(containsFieldWithPolicy("string"));

        System.out.println(containsFieldWithPolicy("age"));

    }


}
