package space.whitememory.pythoninlayparams

import space.whitememory.pythoninlayparams.types.functions.PythonFunctionInlayTypeHintsProvider

class PythonFunctionReturnTypesTest : PythonAbstractInlayHintsTestCase() {

    fun testSimple() = doTest(
        """
        class A:
            ...

        def get_int()<# [->  int] #>:
            return 1
            
        def get_instance()<# [->  A] #>:
            return A()
            
        def get_type()<# [->  [Type [ A ]]] #>:
            return A
            
        def get_union()<# [->  [int  |  str]] #>:
            return 1 or ""
    """.trimIndent()
    )

    fun testNoHint() = doTest(
        """
        def get_implicit_none():
            return
        
        def get_explicit_none():
            return None
            
        def check_ellipsis():
            ...
        
        def check_pass():
            pass
            
        async def async_get_implicit_none():
            return
        
        async def async_get_explicit_none():
            return None
            
        async def async_check_ellipsis():
            ...
        
        async def async_check_pass():
            pass
            
        def check_filled_hint() -> str:
            return ""
 
        def async_check_filled_hint() -> str:
            return ""
    """.trimIndent()
    )

    fun testCollections() = doTest(
        """
        class A:
            ...
            
        def get_set()<# [->  [set [ str ]]] #>:
            return {"foo", "bar"}
            
        def get_tuple()<# [->  [tuple [ int ,  A ]]] #>:
            return 1, A()
        
        def get_nested_tuples()<# [->  [tuple [ [tuple [ int ,  int ]] ,  [tuple [ int ,  int ]] ]]] #>:
            return ((1, 1), (1, 1))
            
        def get_list_any()<# [->  list] #>:
            return []
            
        def get_list_of_union_types()<# [->  [list [ [A  |  int] ]]] #>:
            return [A(), 1]
    """.trimIndent()
    )

    fun testDictionaries() = doTest(
        """    
        def get_dict_any()<# [->  dict] #>:
            return {}
            
        def get_dict_single()<# [->  [dict [ str ,  int ]]] #>:
            return {"": 1}
            
        def get_dict_union()<# [->  [dict [ str ,  [str  |  int] ]]] #>:
            return {"foo": 1, "bar": ""}
            
        def get_nested_dict()<# [->  [dict [ int ,  [dict [ str ,  str ]] ]]] #>:
            return {1: {"": ""}}
    """.trimIndent()
    )

    fun testAsync() = doTest(
        """
        async def coro()<# [->  int] #>:
            return 1
            
        async def get_coro()<# [->  [Coroutine [ Any ,  Any ,  int ]]] #>:
            return coro()
            
        async def get_coro_result()<# [->  int] #>:
            return await coro()
            
        async def test_nested_coro<# [->  [Coroutine [ Any ,  Any ,  [Coroutine [ Any ,  Any ,  int ]] ]]] #>:
            return get_coro()
    """.trimIndent()
    )

    fun testGenerators() = doTest(
        """
        def sync_generator()<# [->  [Generator [ int ,  Any ,  list ]]] #>:
            for i in range(10):
                yield i
                
            return []
            
        async def async_generator()<# [->  [AsyncGenerator [ int ,  Any ]]] #>:
            for i in range(10):
                yield i
    """.trimIndent()
    )

    fun testClassMethods() = doTest(
        """
        class A:
            def check_method_hint()<# [->  int] #>:
                return 1
                
            def check_not_return():
                pass
    """.trimIndent()
    )

    fun testCallable() = doTest(
        """
        def test_callable(foo: int, bar: str) -> int:
            return 1
        
        def get_callable()<# [->  [(foo: int, bar: str)  ->  ( int )]] #>:
            return test_callable
            
        def get_lambda()<# [->  [(x, y)  ->  ( Any )]] #>:
            return lambda x, y: x * y
    """.trimIndent()
    )

    fun testIncompleteDefs() = doTest(
        """
        def
        
        def test  
              
        def test()
        
        def test:
        
        def test() ->
        
        def test() ->:
    """.trimIndent()
    )

    private fun doTest(text: String) {
        testProvider(
            "foo.py", text, PythonFunctionInlayTypeHintsProvider()
        )
    }
}