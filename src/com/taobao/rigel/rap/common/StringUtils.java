package com.taobao.rigel.rap.common;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 为FE提供各类过滤字符串的接口
 * 
 * @author Junquan 2010.01.20
 */
public class StringUtils {

	/**
	 * 默认编码 utf8
	 */
	public static String DEFAULT_CHARSET = "utf8";

	/**
	 * 在html标签或属性中A: 左尖括号：< 转成 &lt; 右尖括号：> 转成 &gt; 单引号：' 转成 &#39; 双引号：" 转成
	 * &quot;
	 */
	public static String escapeInH(String str) {
		if (str == null || ("").equals(str.trim())) {
			return "";
		}

		StringBuffer sb = new StringBuffer();
		int lth = str.length();

		for (int i = 0; i < lth; i++) {
			char c = str.charAt(i);

			switch (c) {

			case 60: // <
				sb.append("&lt;");
				break;
			case 62: // >
				sb.append("&gt;");
				break;
			case 39: // '
				sb.append("&#39;");
				break;
			case 34: // "
				sb.append("&quot;");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return new String(sb.toString());
	}

	public static String escapeInH(Number num) {
		String str = null;
		if (num != null) {
			str = num.toString();
		}

		return escapeInH(str);
	}

	/**
	 * 在html标签或属性中A - 逆
	 */
	public static String UnEscapeInH(String str) {
		// TODO
		return str;
	}

	/**
	 * 在html标签或属性中B: 左尖括号：< 转成 &lt; 右尖括号：> 转成 &gt; 单引号：' 转成 &#39; 双引号：" 转成
	 * &quot; &符号：& 转成&amp;
	 */
	public static String escapeInX(String str) {
		if (str == null || ("").equals(str.trim())) {
			return "";
		}

		StringBuffer sb = new StringBuffer();
		int lth = str.length();

		for (int i = 0; i < lth; i++) {
			char c = str.charAt(i);

			switch (c) {

			case 60: // <
				sb.append("&lt;");
				break;
			case 62: // >
				sb.append("&gt;");
				break;
			case 39: // '
				sb.append("&#39;");
				break;
			case 34: // "
				sb.append("&quot;");
				break;
			case 38: // &
				sb.append("&amp;");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return new String(sb.toString());
	}

	public static String escapeInX(Number num) {
		String str = null;
		if (num != null) {
			str = num.toString();
		}

		return escapeInX(str);
	}

	/**
	 * 在html标签或属性中B - 逆
	 */
	public static String UnEscapeInX(String str) {
		// TODO
		return str;
	}

	/**
	 * 在普通JS环境: 单引号：' 转成 \' 双引号：" 转成 \" 反斜杠：\ 转成 \\ 正斜杠：/ 转成 \/ 换行符 转成 \n 回车符 转成
	 * \r
	 */
	public static String escapeInJ(String str) {
		if (str == null || ("").equals(str.trim())) {
			return "";
		}

		StringBuffer sb = new StringBuffer();
		int lth = str.length();

		for (int i = 0; i < lth; i++) {
			char c = str.charAt(i);

			switch (c) {

			case 39: // '
				sb.append("\\'");
				break;
			case 34: // "
				sb.append("\\\"");
				break;
			case 47: // /
				sb.append("\\/");
				break;
			case 92: // \
				sb.append("\\\\");
				break;
			case 13: // 回车 \r
				sb.append("\\r");
				break;
			case 10: // 换行 \n
				sb.append("\\n");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return new String(sb.toString());
	}

	public static String escapeInJ(Number num) {
		String str = null;
		if (num != null) {
			str = num.toString();
		}

		return escapeInJ(str);
	}

	/**
	 * 在普通JS环境 - 逆
	 */
	public static String UnEscapeInJ(String str) {
		// TODO
		return str;
	}

	/**
	 * 在JS环境的innerHTML: 左尖括号：< 转成 &lt; 右尖括号：> 转成 &gt; 单引号：' 转成 \' 双引号：" 转成 \"
	 * 反斜杠：\ 转成 \\ 正斜杠：/ 转成 \/ 换行符 转成 \n 回车符 转成 \r
	 */
	public static String escapeInJH(String str) {
		if (str == null || ("").equals(str.trim())) {
			return "";
		}

		StringBuffer sb = new StringBuffer();
		int lth = str.length();

		for (int i = 0; i < lth; i++) {
			char c = str.charAt(i);

			switch (c) {

			case 60: // <
				sb.append("&lt;");
				break;
			case 62: // >
				sb.append("&gt;");
				break;
			case 39: // '
				sb.append("\\'");
				break;
			case 34: // "
				sb.append("\\\"");
				break;
			case 47: // /
				sb.append("\\/");
				break;
			case 92: // \
				sb.append("\\\\");
				break;
			case 13: // 回车 \r
				sb.append("\\r");
				break;
			case 10: // 换行 \n
				sb.append("\\n");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return new String(sb.toString());
	}

	public static String escapeInJH(Number num) {
		String str = null;
		if (num != null) {
			str = num.toString();
		}

		return escapeInJH(str);
	}

	/**
	 * 在JS环境的innerHTML - 逆
	 */
	public static String UnEscapeInJH(String str) {
		// TODO
		return str;
	}

	/**
	 * 在标签onclick等事件函数参数中: 左尖括号：< 转成 &lt; 右尖括号：> 转成 &gt; &符号：& 转成&amp; 单引号：' 转成
	 * \&#39; 双引号：" 转成 \&quot; 反斜杠：\ 转成 \\ 正斜杠：/ 转成 \/ 换行符 转成 \n 回车符 转成 \r
	 */
	public static String escapeInHJ(String str) {
		if (str == null || ("").equals(str.trim())) {
			return "";
		}

		StringBuffer sb = new StringBuffer();
		int lth = str.length();

		for (int i = 0; i < lth; i++) {
			char c = str.charAt(i);

			switch (c) {

			case 60: // <
				sb.append("&lt;");
				break;
			case 62: // >
				sb.append("&gt;");
				break;
			case 39: // '
				sb.append("\\&#39;");
				break;
			case 34: // "
				sb.append("\\&quot;");
				break;
			case 38: // &
				sb.append("&amp;");
				break;
			case 47: // /
				sb.append("\\/");
				break;
			case 92: // \
				sb.append("\\\\");
				break;
			case 13: // 回车 \r
				sb.append("\\r");
				break;
			case 10: // 换行 \n
				sb.append("\\n");
				break;
			default:
				sb.append(c);
				break;
			}
		}
		return new String(sb.toString());
	}

	public static String escapeInHJ(Number num) {
		String str = null;
		if (num != null) {
			str = num.toString();
		}

		return escapeInHJ(str);
	}

	/**
	 * 在标签onclick等事件函数参数中 - 逆
	 */
	public static String UnEscapeInHJ(String str) {
		// TODO
		return str;
	}

	/**
	 * 在URL参数中: 对非字母、数字字符进行转码(%加字符的ASCII格式)
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public static String escapeInU(String str)
			throws UnsupportedEncodingException {
		if (str == null || ("").equals(str.trim())) {
			return "";
		}
		return URLEncoder.encode(str, DEFAULT_CHARSET);
	}

	/**
	 * 在URL参数中 - 逆
	 * 
	 * @throws UnsupportedEncodingException
	 */
	public static String UnEscapeInU(String str)
			throws UnsupportedEncodingException {
		if (str == null || ("").equals(str.trim())) {
			return "";
		}
		return URLDecoder.decode(str, DEFAULT_CHARSET);
	}

	public static String getMD5(String src) {
		byte[] defaultBytes = src.getBytes();
		StringBuffer hexString = new StringBuffer();
		try {
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(defaultBytes);
			byte messageDigest[] = algorithm.digest();

			for (int i = 0; i < messageDigest.length; i++) {
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return hexString.toString();
	}

	public static String getMD5(byte[] source) {
		String s = null;
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };// 用来将字节转换成16进制表示的字符
		try {
			java.security.MessageDigest md = java.security.MessageDigest
					.getInstance("MD5");
			md.update(source);
			byte tmp[] = md.digest();// MD5 的计算结果是一个 128 位的长整数，
			// 用字节表示就是 16 个字节
			char str[] = new char[16 * 2];// 每个字节用 16 进制表示的话，使用两个字符， 所以表示成 16
			// 进制需要 32 个字符
			int k = 0;// 表示转换结果中对应的字符位置
			for (int i = 0; i < 16; i++) {// 从第一个字节开始，对 MD5 的每一个字节// 转换成 16
				// 进制字符的转换
				byte byte0 = tmp[i];// 取第 i 个字节
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];// 取字节中高 4 位的数字转换,// >>>
				// 为逻辑右移，将符号位一起右移
				str[k++] = hexDigits[byte0 & 0xf];// 取字节中低 4 位的数字转换

			}
			s = new String(str);// 换后的结果转换为字符串

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return s;
	}

	public static String getDoubleMD5(String src) {
		if (src != null) {
			src = getMD5(src);
			src = getMD5(src);
		}
		return src;
	}

	/**
	 * 把中文转成Unicode码
	 * 
	 * @param str
	 * @return
	 */
	public static String chineseToUnicode(String str) {
		if (str == null) {
			str = "";
		}
		String result = "";
		for (int i = 0; i < str.length(); i++) {
			int chr1 = (char) str.charAt(i);
			if (chr1 >= 19968 && chr1 <= 171941) {// 汉字范围 \u4e00-\u9fa5 (中文)
				result += "\\u" + Integer.toHexString(chr1);
			} else {
				result += str.charAt(i);
			}
		}
		return result;
	}

	/**
	 * 判断是否为中文字符
	 * 
	 * @param c
	 * @return
	 */
	public static boolean isChinese(char c) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
				|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
			return true;
		}
		return false;
	}

	/**
	 * regular expression matcher helper
	 * 
	 * @param pattern
	 *            regular expression
	 * @param str
	 *            string to be matched
	 * @return
	 */
	public static boolean regMatch(String pattern, String str) {
		Pattern p = Pattern.compile(pattern);
		Matcher matcher = p.matcher(str);
		return matcher.matches();
	}

	/**
     * 得到格式化json数据  退格用\t 换行用\r
     */
    public static String formatJSON(String jsonStr) {
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
}
