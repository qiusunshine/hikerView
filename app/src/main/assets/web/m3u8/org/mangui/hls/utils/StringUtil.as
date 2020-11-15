package static.m3u8.org.mangui.hls.utils
{
	public class StringUtil
	{
		/**
		 * Returns <code>true</code> if the specified string is
		 * a single space, tab, carriage return, newline, or formfeed character.
		 *
		 * @param str The String that is is being queried. 
		 *
		 * @return <code>true</code> if the specified string is
		 * a single space, tab, carriage return, newline, or formfeed character.
		 */
		public static function isWhitespace(character:String):Boolean
		{
			switch (character)
			{
				case " ":
				case "\t":
				case "\r":
				case "\n":
				case "\f":
				case "\u00A0": // non breaking space
				case "\u2028": // line seperator
				case "\u2029": // paragraph seperator
				case "\u3000": // ideographic space
					return true;
			}
			
			return false;
		}
		
		/**
		 * Removes all whitespace characters from the beginning and end
		 * of the specified string.
		 *
		 * @param str The String whose whitespace should be trimmed. 
		 *
		 * @return Updated String where whitespace was removed from the 
		 * beginning and end. 
		 */
		public static function trim(str:String):String
		{
			if (str == null) return '';
			
			var startIndex:int = 0;
			while (isWhitespace(str.charAt(startIndex)))
				++startIndex;
			
			var endIndex:int = str.length - 1;
			while (isWhitespace(str.charAt(endIndex)))
				--endIndex;
			
			if (endIndex >= startIndex)
				return str.slice(startIndex, endIndex + 1);
			else
				return "";
		}
		
		/**
		 * Splits a String into an Vector of nicely trimmed strings, where each
		 * item represents a single line of the original String data
		 * 
		 * @param	str		The String to be split into an Array
		 * @return			Vector of type String 
		 */
		public static function toLines(str:String):Vector.<String>
		{
			var lines:Array = toLF(str).split("\n");
			var i:uint;
			var length:uint = lines.length;
			
			for (i=0; i<length; ++i)
			{
				lines[i] = trim(lines[i]);
			}
			
			return Vector.<String>(lines);
		}
		
		/**
		 * Converts strings containing Windows (CR-LF), MacOS (CR) and other 
		 * non-standard line breaks (LF-CR) into strings using only Linux-style
		 * line breaks (LF).
		 * 
		 * @param	str		String containing non-Linux line breaks
		 * @returns			String containly only Linux-style line breaks 
		 */
		public static function toLF(str:String):String
		{
			return (str || "").replace(/\r\n|\n\r|\r/g, "\n");
		}
	}
}