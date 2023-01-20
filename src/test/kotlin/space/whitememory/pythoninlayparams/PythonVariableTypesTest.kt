package space.whitememory.pythoninlayparams

import space.whitememory.pythoninlayparams.types.variables.PythonVariablesInlayTypeHintsProvider

class PythonVariableTypesTest : PythonAbstractInlayHintsTestCase() {
    private val testObjects = """
        import datetime
        from typing import Union, TypedDict

        def get_int():
            return 1
            
        class TestTyped(TypedDict):
            id: int
            name: str
            
        class UnionChild(Union):
            ...

        class A:
            def __init__(self, val: int):
                self.val = val

            @classmethod
            def from_val(cls, val: int) -> "A":
                return cls(val)
                
        def get_a():
            return A()
        
        def get_a_type():
            return A
                
        def get_optional():
            if True:
                return [A(1)]
        
            return None
            
        def get_callable():
            return get_int
            
        def get_lambda():
            return lambda x, y: x * y
            
        def get_typed_dict() -> TestTyped:
            return {"id": 1, name: "test"}
            
        async def coro():
            return 1
            
        async def get_coro():
            return coro()
            
        def get_now():
            return datetime.datetime.now()
            
        def get_any_dict():
            return {}
            
        def get_complex_dict():
            return {1: A(), 2: None, 3: [], 4: tuple()}
            
        def get_complex_list():
            return [1, [[], 1, '3'], '', []]
    """.trimIndent()

    fun testSimple() = doTest(
        """
        x1<# [:  int] #> = get_int()
        x2<# [:  [[list [ A ]]  |  None]] #> = get_optional()
        x3<# [:  TestTyped] #> = get_typed_dict()
        x4<# [:  dict] #> = get_any_dict()
        
        x7<# [:  [A  |  None]] #> = A() or None
        x8<# [:  [A  |  None]] #> = A() and None
        x9<# [:  A] #> = A() or get_a()
        x10<# [:  [A  |  None]] #> = get_a() or None
        x11<# [:  [A  |  None]] #> = A.from_val(10) or None
        x12<# [:  [[Type [ A ]]  |  A]] #> = get_a_type() or A()
        x13<# [:  [Type [ A ]]] #> = get_a_type()
        
        b1<# [:  datetime] #> = get_now()
    """.trimIndent()
    )

    fun testNoHint() = doTest(
        """
        x1 = 1
        x2 = -1
        x3 = +1
        x4 = 1_000
        x5 = 1 or 2
        x6 = 1 and 2
        x7 = 1 if True else 1
        x8 = 1 + 1
        x9 = 1 - 1
 
        b = (111)
        c = (1,)
        d = ("1", "2", 3)
        e = []
            
        f: int = get_int()
        # g1 = A
        g2 = A(1)
        g3 = A(1) or A(2)
        g4 = A.from_val(1)
        g5 = Union[int, str]
        # g6 = UnionChild[int, str]

        g7 = {k: None for k, _ in range(10)}
        g8 = {1: A(), 2: None, 3: [], 4: tuple()}
        
        h1 = datetime.datetime.now()
        h2 = datetime.datetime()
    """.trimIndent()
    )

    fun testReassignments() = doTest(
        """
        x<# [:  [Coroutine [ Any ,  Any ,  int ]]] #> = coro()
        y<# [:  [Coroutine [ Any ,  Any ,  int ]]] #> = x
        z<# [:  int] #> = await y
        
        f = 1
        g = f
        h<# [:  int] #> = 1 + g
    """.trimIndent()
    )

    fun testComplex() = doTest(
        """
        x1<# [:  [[list [ A ]]  |  None  |  list  | ...]] #> = get_optional() or [] or {}
        complex_list<# [:  [list [ [int  |  [list [ [list  |  int  | ...] ]]  | ...] ]]] #> = get_complex_list()
        complex_dict<# [:  [dict [ int ,  [A  |  None  |  list  | ...] ]]] #> = get_complex_dict()
        
        
        coro_callable<# [:  [()  ->  ( [Coroutine [ Any ,  Any ,  int ]] )]] #> = coro
        one_coro<# [:  [Coroutine [ Any ,  Any ,  int ]]] #> = await get_coro()
        nested_coro<# [:  [Coroutine [ Any ,  Any ,  [Coroutine [ Any ,  Any ,  int ]] ]]] #> = get_coro()
        coro_value<# [:  int] #> = await one_coro
        
        x4<# [:  [()  ->  ( int )]] #> = get_callable()
        x5<# [:  [(x, y)  ->  ( Any )]] #> = get_lambda()
        x6<# [:  [(val: int)  ->  ( A )]] #> = A.from_val
        
        def get_generator():
            for i<# [:  int] #> in range(10):
                yield i
            return []
        
        async def get_async_generator():
            for i<# [:  int] #> in range(10):
                yield i
        
        g1<# [:  [Generator [ int ,  Any ,  list ]]] #> = get_generator()
        g2<# [:  [AsyncGenerator [ int ,  Any ]]] #> = get_async_generator()
    """.trimIndent()
    )

    fun testClassAttrs() = doTest(
        """
        class TestAttrs:
            x<# [:  int] #> = get_int()
            y<# [:  [[list [ A ]]  |  None]] #> = get_optional()
            x = 1
    """.trimIndent()
    )

    fun testLoopHints() = doTest(
        """
        dt = {"test": 1}
        
        d3 = {u: n for u<# [:  str] #>, n<# [:  int] #> in dt.items()}
        d<# [:  [list [ int ]]] #> = [i for i<# [:  int] #> in range(10)]
        s2 = {i for i<# [:  int] #> in range(10)}
        d2 = {k: v for k<# [:  str] #>, v<# [:  int] #> in {"a": 1, "b": 2}.items()}
        c<# [:  [list [ A ]]] #> = [get_a() for i<# [:  int] #> in range(10)]
        s = {get_a_type() for i<# [:  int] #> in range(10)}
        v = {k: get_a() for k<# [:  int] #> in range(10)}
        s1<# [:  [frozenset [ A ]]] #> = frozenset(get_a() for i<# [:  int] #> in range(10))
    """.trimIndent()
    )

    fun testIterableHints() = doTest(
        """
        item_set<# [:  [set [ str ]]] #> = set()
        x<# [:  None] #> = item_set.add("1")
        
        explicit_item_set: set[str] = set()
        y<# [:  None] #> = explicit_item_set.add("1")
        
        unclear_item_set: set = set()
        z<# [:  None] #> = unclear_item_set.add("1")
    """.trimIndent()
    )

    private fun doTest(text: String) {
        testProvider(
            "foo.py",
            "$testObjects\n$text",
            PythonVariablesInlayTypeHintsProvider(),
            PythonVariablesInlayTypeHintsProvider.Settings(showClassAttributeHints = true, showGeneralHints = true)
        )
    }
}