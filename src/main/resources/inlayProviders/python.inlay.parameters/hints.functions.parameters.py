def multiply(first: int, second: int) -> int:
    return first * second


def find_max(*args: int) -> int:
    return max(args)


one_result = multiply(2, 3)
other_result = multiply(4, second=5)
find_max(one_result, other_result)
