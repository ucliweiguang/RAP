package com.taobao.rigel.rap.common;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {

    //@Test
    public void estEscapeInH() throws Exception {
        String str = StringUtils.escapeInH("<>");
        assertEquals(str, "&lt;&gt;");
        // assertEquals(str + "12", "&lt;&gt;");
    }
    @Test
    public void testString() throws UnsupportedEncodingException{
    	String jsonStr = "{\"id\":\"1\",\"name\":\"a1\",\"obj\":{\"id\":11,\"name\":\"a11\",\"array\":[{\"id\":111,\"name\":\"a111\"},{\"id\":112,\"name\":\"a112\"}]}}";
        String fotmatStr = format(jsonStr);
//    		fotmatStr = fotmatStr.replaceAll("\n", "<br/>");
//    		fotmatStr = fotmatStr.replaceAll("\t", "    ");
        System.out.println(fotmatStr);
    }
    
    /**
     * 得到格式化json数据  退格用\t 换行用\r
     */
    private static String format(String jsonStr) {
      int level = 0;
      StringBuffer jsonForMatStr = new StringBuffer();
      for(int i=0;i<jsonStr.length();i++){
        char c = jsonStr.charAt(i);
        if(level>0&&'\n'==jsonForMatStr.charAt(jsonForMatStr.length()-1)){
          jsonForMatStr.append(getLevelStr(level));
        }
        switch (c) {
        case '{': 
        case '[':
          jsonForMatStr.append(c+"\n");
          level++;
          break;
        case ',': 
          jsonForMatStr.append(c+"\n");
          break;
        case '}':
        case ']':
          jsonForMatStr.append("\n");
          level--;
          jsonForMatStr.append(getLevelStr(level));
          jsonForMatStr.append(c);
          break;
        default:
          jsonForMatStr.append(c);
          break;
        }
      }
      
      return jsonForMatStr.toString();

    }
    
    private static String getLevelStr(int level){
      StringBuffer levelStr = new StringBuffer();
      for(int levelI = 0;levelI<level ; levelI++){
        levelStr.append("\t");
      }
      return levelStr.toString();
    }
    
	public String convert(String utfString){  
	    StringBuilder sb = new StringBuilder();  
	    int i = -1;  
	    int pos = 0;  
	      
	    while((i=utfString.indexOf("\\u", pos)) != -1){  
	        sb.append(utfString.substring(pos, i));  
	        if(i+5 < utfString.length()){  
	            pos = i+6;  
	            sb.append((char)Integer.parseInt(utfString.substring(i+2, i+6), 16));  
	        }  
	    }  
	    sb.append(utfString.substring(pos));
	    return sb.toString();  
	}  
}