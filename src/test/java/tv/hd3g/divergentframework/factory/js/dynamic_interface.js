/**
 * JavaScript dynamic interface instanciation
 */

{
	/**
	 * void simple();
	 */
	simple = function() {
		test1();
	},
	/**
	 * String stringSupplier();
	 */
	stringSupplier = function() {
		return "Hello world!";
	},
	/**
	 * void intConsumer(int value);
	 */
	intConsumer = function(value) {
		test2(value);
	},
	/**
	 * long biConsumer(long value1, long value2);
	 */
	biConsumer = function(value1, value2) {
		return value1 * value;
	},
	/**
	 * int stringToIntFunction(String value);
	 */
	stringToIntFunction = function(value) {
		return parseInt(value);
	},
	/**
	 * int varArgs(int... values);
	 */
	varArgs = function(values) {
		var result = 0;
		for (var pos = 0; pos < values.length; pos++) {
			result += values[pos];
		}
		return result;
	},
	/**
	 * default boolean aDefault()
	 */
	aDefault = function() {
		return false;
	},
	/**
	 * List<String> stringList(ArrayList<String> values);
	 */
	stringList = function(values) {
		var result = [];
		for (var pos = 0; pos < values.length; pos++) {
			result.push(values[pos].toUpperCase());
		}
		return result;
	},
	/**
	 * Map<String, Integer> intMap(HashMap<String, Integer> values);
	 */
	intMap = function(values) {
		return {
			v1: values["v1"] * 2,
		};
	}
}


