## Quick Settings - configure common code-style and other settings here. -----------------------------------------------
## Set these to use special prefixes for test-class members containing dependencies of certain types.
## Note that the template code below will ignore any prefix set to "" and use your IDE code style settings instead.
#set($dependencyMemberNamePrefix = "")
#set($mockDependencyMemberNamePrefix = "mock")
## Set this to use a special prefix for local-fields containing arguments for test-methods.
#set($parameterLocalFieldNamePrefix = "")
#set($mockParameterLocalFieldNamePrefix = "mock")
## Customize the name of the member and local-field (if needed) used to store the instance of the source-class.
#set($sourceClass.testClassMemberName = "${sourceClass.testClassMemberName}UnderTest")
#set($sourceClass.testClassLocalFieldName = "${sourceClass.testClassLocalFieldName}UnderTest")
#set($useStaticImportForInitMocks = true)
## Set this to use mocks for mockable method parameters that end in "listener" or "callback" ignoring case.
#set($useMocksForListenerAndCallbackParameters = true)
## Use this to specify custom initialization values for dependencies and method parameters of certain types;
## See https://squaretest.com#template_api_quick_settings for details.
#set($initExpressionOverrides = {} )
##----------------------------------------------------------------------------------------------------------------------

## Initialize the data model. This sets global variables based on the architype of the source-class and Quick Settings.
## See the comments above the macro or https://squaretest.com#template_api_initializeTemplateDataModel for details.
#initializeTemplateDataModel()

## Package declaration
#if($StringUtils.isNotEmpty($sourceClass.packageName))
package $sourceClass.packageName;
#end

## Imports; Note that Squaretest invokes IntelliJ's import organize and code reformatter after creating the test-class.
#foreach($importLine in $importLinesRequired)
    $importLine
#end
#if($useStaticImportForInitMocks)
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.Mockito.mock;
#end
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.InjectMocks;

## Test class
public class ${sourceClass.name}Test {

## Declare member fields for the dependencies.
#renderMemberDeclarations($mockMemberFields)
#renderMemberDeclarations($nonMockMemberFields)

## Declare a member field for the instance of the source-class.
#if($sourceClassMemberNeeded)
    #if($shouldUseInjectMocks) @InjectMocks #end
    private ${sourceClass.type.canonicalText} ${sourceClass.testClassMemberName};
#end

## Render setUp() only if we need to instantiate source-class.
#if($sourceClassMemberNeeded)
@Before
public void setUp() {
    #if($mocksNeeded)
        #if(!$useStaticImportForInitMocks)MockitoAnnotations.#end initMocks(this);
    #end
    ## Initialize the non-mock member fields.
    #foreach($field in $nonMockMemberFields)
        $field.testClassMemberName = $field.initExpression;
    #end
    ## Initialize the member containing the source-class.
    #if($sourceClass.singleton)
        ${sourceClass.testClassMemberName} = $sourceClass.singletonAccessExpression;
    #elseif($shouldUseInjectMocks) ## Do nothing.
    #elseif($sourceClass.preferredConstructor)
        ## Invoke the constructor.
        ${sourceClass.testClassMemberName} = new #methodCall($sourceClass.preferredConstructor, true) #if($sourceClass.abstract)$sourceClass.abstractClassBody#end;
        #if($shouldSetPackageLocalFields)
            #foreach($field in $dependencies)
                ${sourceClass.testClassMemberName}.${field.declaredName} = #if($field.shouldStoreInReference) $field.testClassMemberName #else $field.initExpression #end;
            #end
        #end
    #else
        ## As a fallback, set the member to null and leave a comment.
        ${sourceClass.testClassMemberName} = null /* TODO: construct the instance */;
    #end
}
#end
## Render the test methods.
#if($shouldCreateTestsForInstanceMethods)
    #renderTestMethods($sourceClass.publicInstanceMethods)
    #renderTestMethods($sourceClass.packageLocalInstanceMethods)
#end
#renderTestMethods($sourceClass.publicStaticMethods)
#renderTestMethods($sourceClass.packageLocalStaticMethods)
}

## Macros
## Add your own macros here. Newlines between macros will not appear in the generated test-class;
## Squaretest automatically removes all but 1 newline at the end of the generated test-class.

##----------------------------------------------------------------------------------------------------------------------
## renderMemberDeclarations($fields)
## Renders test-class member declarations for the given List<Variable>.
## Param: $fields - the list of variables for which to render declarations.
##----------------------------------------------------------------------------------------------------------------------
#macro(renderMemberDeclarations $fields)
    #foreach($field in $fields)
        #if($field.shouldBeMocked) @Mock #end
        private $field.type.canonicalText $field.testClassMemberName;
    #end
#end

##----------------------------------------------------------------------------------------------------------------------
## renderTestMethods($methods)
## Renders tests for the given List<Method>. This filters out getters/setters and other methods for which tests should
## not be rendered.
## Param: $methods - the list of methods for which to render tests.
##----------------------------------------------------------------------------------------------------------------------
#macro(renderTestMethods $methods)
    #foreach($method in $methods)
        #if(!$method.simpleGetterOrSetter && !$method.abstract)
            #renderTestMethod($method)
        #end
    #end
#end

##----------------------------------------------------------------------------------------------------------------------
## renderTestMethod($method)
## Renders the test for the given Method.
## Param: $method - the method for which to render the test.
##----------------------------------------------------------------------------------------------------------------------
#macro(renderTestMethod $method)
#if($sourceClass.enum)
    #renderTestMethodForEnum($method)
#else
    @Test
    public void test${StringUtils.capitalize(${method.name})}${method.overloadSuffix}() #if($method.throwsException) throws Exception #end {
    // Given #* sq_hint:setup_comment *#
    ## Declare parameters to use to invoke the method.
    #foreach($param in $ListUtils.filter($method.parameters, "shouldStoreInReference", true))
        $param.type.canonicalText $param.testClassLocalFieldName = $param.initExpression;
    #end
    #if($method.returnType)
        $method.returnType.canonicalText expectedResult = $method.returnType.initExpression;
    #end

    // When #* sq_hint:run_comment *#
    #if($method.static)
        #set($qualifier = $sourceClass.name)
    #else
        #set($qualifier = $sourceClass.testClassMemberName)
    #end
    #if($method.returnType) $method.returnType.canonicalText result = #end ${qualifier}.#methodCall($method, false);

    // Then #* sq_hint:verify_comment *#
    #if($method.returnType)
        #assertEqualsCall('expectedResult', 'result', $method.returnType);
    #end
    }
#end
#end

##----------------------------------------------------------------------------------------------------------------------
## assertEqualsCall($expected, $actual, $type)
## Renders the assertEquals method call appropriate for the given parameters.
## Param: $expected - the value to use for the expected argument of the assertEquals method call.
## Param: $actual - the value to use for the actual argument of the assertEquals method call.
## Param: $type - the Type of the arguments; this is used to determine which assertEquals method to call.
##----------------------------------------------------------------------------------------------------------------------
#macro(assertEqualsCall $expected, $actual, $type)
    #if($type.array) assertArrayEquals($expected, $actual) #break ## break is needed to avoid adding a newline.
    #elseif('double' == $type.canonicalName || 'java.lang.Double' == $type.canonicalName) assertEquals($expected, $actual, 0.0001) #break
    #elseif('float' == $type.canonicalName || 'java.lang.Float' == $type.canonicalName) assertEquals($expected, $actual, 0.0001) #break
    #else assertEquals($expected, $actual) #end
#end

##----------------------------------------------------------------------------------------------------------------------
## methodCall($method, $assumeArgsAreStoredInMembers)
## Renders an expression to call the given method. This does not include any qualifiers required to reference
## the method.
## Param: $method - the method or constructor for which to render the call-expression.
## Param: $assumeArgsAreStoredInMembers (optional) - assumes the arguments are stored in member-fields instead of
##        local-fields.
##----------------------------------------------------------------------------------------------------------------------
#macro(methodCall $method, $assumeArgsAreStoredInMembers)
    ${method.name}#if($method.constructor && $sourceClass.hasGenerics)<>#end
    (#foreach($param in $method.parameters)
        #if(!$param.shouldStoreInReference) $param.initExpression #elseif($assumeArgsAreStoredInMembers) $param.testClassMemberName #else $param.testClassLocalFieldName #end #if($foreach.hasNext),#end #end) #end

##----------------------------------------------------------------------------------------------------------------------
## renderTestMethodForEnum($method)
## Renders the enum test-method for the given Method. This is similar to #renderTestMethod($method) but calls the
## method on each value in the enum and adds an assertEquals call for each result.
## Param: $method - the method for which to render the test.
##----------------------------------------------------------------------------------------------------------------------
#macro(renderTestMethodForEnum $method)
    #set($multipleValuesTest = $sourceClass.enumValues.size() > 1 && !$method.static)
    #set($paramsToStoreInFields = $ListUtils.filter($method.parameters, "shouldStoreInReference", true))
    #set($shouldRenderSetup = $paramsToStoreInFields.size() > 0 || ($method.returnType && !$multipleValuesTest))
    @Test
    public void test${StringUtils.capitalize(${method.name})}${method.overloadSuffix}() #if($method.throwsException) throws Exception #end {
        #if($shouldRenderSetup)
            // Given
            ## Declare parameters to use to invoke the method.
            #foreach($param in $paramsToStoreInFields)
                $param.type.canonicalText $param.testClassLocalFieldName = $param.initExpression;
            #end
            #if($method.returnType)
                $method.returnType.canonicalText expectedResult = $method.returnType.initExpression;
            #end

        #end
        // When
        #if($method.static)
            #set($qualifier = $sourceClass.name)
        #else
            #set($qualifier = "${sourceClass.name}.${sourceClass.enumFirstValue}")
        #end
        #if($multipleValuesTest)
            #foreach($enumValue in $sourceClass.enumValues)
                #if($method.returnType) $method.returnType.canonicalText ${enumValue.toLowerCase()}Result = #end ${sourceClass.name}.${enumValue}.#methodCall($method, false);
            #end
        #else
            #if($method.returnType) $method.returnType.canonicalText result = #end ${qualifier}.#methodCall($method, false);
        #end

        // Then
        #if($multipleValuesTest)
            #foreach($enumValue in $sourceClass.enumValues)
                #assertEqualsCall("$method.returnType.initExpression", "${enumValue.toLowerCase()}Result", $method.returnType);
            #end
        #elseif($method.returnType)
            #assertEqualsCall('expectedResult', 'result', $method.returnType);
        #end
    }
#end

##----------------------------------------------------------------------------------------------------------------------
## initializeTemplateDataModel()
## Updates mutable fields in $sourceClass and $importLinesRequired based on the Quick Settings at the top of the file
## and the architype of the source-class. This also sets additional variables used in the rendering logic.
## Using a macro to prepare the data model separates the test-class rendering-logic from the configuration and
## class architype-detection logic; this keeps the rendering logic simple and easy to modify.
##
## Global Variables Set by #initializeTemplateDataModel:
##
##   $dependencies                  a List<Variable> containing all fields the test-class should provide for the
##                                  instance of the source-class.
##   $memberFields                  a subset of $dependencies containing only fields that should be stored in members
##                                  in the test-class.
##   $mockMemberFields              a subset of $memberFields containing only fields that should be mocked.
##
##   $nonMockMemberFields           a subset of $memberFields containing only fields that should not be mocked.
##
##   $sourceClassMemberNeeded       a boolean indicating whether or not an instance of the source-class should be
##                                  created and stored in a member of the test-class.
##   $shouldUseInjectMocks          a boolean indicating whether or not @InjectMocks should be used to provide
##                                  dependencies to the instance of the source-class.
##   $shouldSetPackageLocalFields   a boolean indicating whether or not the test-class should provide dependencies
##                                  to the source-class by setting package-local fields.
##   $mocksNeeded                   true if source-class has at least one dependency that should be mocked.
##
##   $shouldCreateTestsForInstanceMethods
##                                  a boolean indicating whether or not test should be created for the instance methods
##                                  in the source-class.
##----------------------------------------------------------------------------------------------------------------------
#macro(initializeTemplateDataModel)
    ## Determine the archetype of the source-class and set the global variables accordingly.
    #set($shouldCreateTestsForInstanceMethods = true)
    #set($mutablePackageLocalInstanceFields = $ListUtils.filter($sourceClass.packageLocalInstanceFields, "final", false))
    #if($sourceClass.enum)
        #set($dependencies = [])
        #set($sourceClassMemberNeeded = false)
    #elseif($sourceClass.singleton)
        #set($dependencies = [])
        #set($sourceClassMemberNeeded = true)
    #elseif($sourceClass.instanceMethods.empty && !$sourceClass.staticMethods.empty)
        ## The source-class only has static methods; it's likely a utils class.
        #set($dependencies = [])
        #set($sourceClassMemberNeeded = false)
    #elseif($sourceClass.preferredConstructor && !$sourceClass.preferredConstructor.parameters.empty)
        ## The source-class has a parameterized constructor; use it to provide the dependencies.
        #set($dependencies = $sourceClass.preferredConstructor.parameters)
        #set($sourceClassMemberNeeded = true)
    #elseif($sourceClass.preferredConstructor && !$ListUtils.filter($sourceClass.dependencyAnnotatedFields, "private", true).empty)
        ## The source-class has a no-args constructor and private fields annotated with @Inject or @Autowired.
        #set($shouldUseInjectMocks = true)
        #set($dependencies = $sourceClass.dependencyAnnotatedFields)
        #set($sourceClassMemberNeeded = true)
    #elseif($sourceClass.preferredConstructor && !$sourceClass.dependencyAnnotatedFields.empty)
        ## The source-class has a no-args constructor and visible, dependency-annotated fields;
        ## the test-class should set them.
        #set($dependencies = $sourceClass.dependencyAnnotatedFields)
        #set($shouldSetPackageLocalFields = true)
        #set($sourceClassMemberNeeded = true)
    #elseif($sourceClass.preferredConstructor && !$mutablePackageLocalInstanceFields.empty)
        ## The source-class has a no-args constructor and mutable package-local instance fields; assume they are
        ## dependencies and have the test-class set them.
        #set($dependencies = $mutablePackageLocalInstanceFields)
        #set($shouldSetPackageLocalFields = true)
        #set($sourceClassMemberNeeded = true)
    #elseif($sourceClass.preferredConstructor)
        ## The source-class has a visible constructor but no package-local fields or dependency-annotated fields.
        #set($dependencies = [])
        #set($sourceClassMemberNeeded = true)
    #elseif(!$sourceClass.packageVisibleStaticCreatorMethods.empty)
        ## The source-class has no visible constructor, but does have static creator methods; e.g. parse(...), from(...).
        #set($dependencies = [])
        #set($sourceClassMemberNeeded = false)
        #set($shouldCreateTestsForInstanceMethods = false)
    #else
        #set($dependencies = [])
        #set($sourceClassMemberNeeded = true)
    #end
    ## Update the mutable fields on the dependencies based on the Quick Settings.
    #setDependencyNamesAndInitExpressions($dependencies, $shouldUseInjectMocks, $shouldSetPackageLocalFields)
    ## Filter out fields that should not be stored as members in the test-class.
    #set($memberFields = ${ListUtils.filter($dependencies, "shouldStoreInReference", true)})
    #set($mockMemberFields = ${ListUtils.filter($memberFields, "shouldBeMocked", true)})
    #set($nonMockMemberFields = ${ListUtils.filter($memberFields, "shouldBeMocked", false)})
    #set($mocksNeeded = !$mockMemberFields.empty || $shouldUseInjectMocks)
    #initializeMethodParamsWithQuickSettings()
#end

##----------------------------------------------------------------------------------------------------------------------
## initializeMethodParamsWithQuickSettings()
## Sets mutable fields on the method parameters based on Quick Settings.
##----------------------------------------------------------------------------------------------------------------------
#macro(initializeMethodParamsWithQuickSettings)
    #foreach($method in $sourceClass.methods)
    #foreach($param in $method.parameters)
        ## If useMocksForListenerAndCallbackParameters is set it takes precedence over any initExpressionOverrides.
        #if($useMocksForListenerAndCallbackParameters
            && $param.type.mockable
            && ($StringUtils.endsWithIgnoreCase($param.declaredName, "listener")
                || $StringUtils.endsWithIgnoreCase($param.declaredName, "callback")))
            #useInlineMockForParam($param)
        #else
            ## Check the initExpressionOverrides.
            #set($_initExpressionOverride = false)
            #set($_initExpressionOverride = $initExpressionOverrides.get($param.type.canonicalName))
            #if($_initExpressionOverride)
                #set($param.initExpression = $_initExpressionOverride.initExpression)
                #set($param.shouldStoreInReference = $_initExpressionOverride.shouldStoreInReference)
                #set($param.shouldBeMocked = false)
                #set($_ = $importLinesRequired.addAll($ListUtils.nullToEmpty($_initExpressionOverride.importsRequired)))
            #else
                #set($param.shouldBeMocked = false)
            #end
        #end
        #setTestClassNamesForParam($param)
    #end
    #end
#end

##----------------------------------------------------------------------------------------------------------------------
## setDependencyNamesAndInitExpressions($params, $shouldUseInjectMocks, $shouldSetPackageLocalFields)
## Sets mutable fields on the $params based on the Quick Settings.
## Param: $params the List<Variable> containing the dependencies.
## Param: $shouldUseInjectMocks
##          a boolean indicating whether or not @InjectMocks should be used to provide dependencies to the instance
##          of the source-class.
## Param: $shouldSetPackageLocalFields
##          a boolean indicating whether or not the test-class should provide dependencies to the source-class by
##          setting package-local fields.
##----------------------------------------------------------------------------------------------------------------------
#macro(setDependencyNamesAndInitExpressions $dependencies $shouldUseInjectMocks $shouldSetPackageLocalFields)
    #foreach($dependency in $dependencies)
        ## Use the initExpressionOverride if one is present and we do not need to use @InjectMocks.
        #set($_initExpressionOverride = false)
        #set($_initExpressionOverride = $initExpressionOverrides.get($dependency.type.canonicalName))
        #if($_initExpressionOverride && !$shouldUseInjectMocks)
            #set($dependency.initExpression = $_initExpressionOverride.initExpression)
            #set($dependency.shouldStoreInReference = $_initExpressionOverride.shouldStoreInReference)
            #set($dependency.shouldBeMocked = false)
            #set($_ = $importLinesRequired.addAll($ListUtils.nullToEmpty($_initExpressionOverride.importsRequired)))
        #elseif($shouldUseInjectMocks && $dependency.type.mockable)
            #set($dependency.shouldBeMocked = true)
        #end
        ## If the dependencies are provided by setting package-local fields, set them inline instead of storing
        ## references to them in the test-class.
        #if($shouldSetPackageLocalFields)
            #if($dependency.shouldBeMocked)
                #useInlineMockForParam($dependency)
            #end
            #set($dependency.shouldStoreInReference = false)
        #end
        #setTestClassNamesForParam($dependency)
    #end
#end

##----------------------------------------------------------------------------------------------------------------------
## setTestClassNamesForParam($param)
## Sets the Variable.testClassMemberName and Variable.testClassLocalFieldName properties on the given $param
## based on the Quick Settings.
## Param: $param the Variable to update
##----------------------------------------------------------------------------------------------------------------------
#macro(setTestClassNamesForParam $param)
    #if($param.shouldBeMocked)
        #if(${StringUtils.isNotEmpty($mockDependencyMemberNamePrefix)})
            #set($param.testClassMemberName = "${mockDependencyMemberNamePrefix}${StringUtils.capitalize(${param.declaredNameWithoutPrefix})}")
        #elseif (${StringUtils.isNotEmpty($dependencyMemberNamePrefix)})
            #set($param.testClassMemberName = "${dependencyMemberNamePrefix}${StringUtils.capitalize(${param.declaredNameWithoutPrefix})}")
        #end
        #if(${StringUtils.isNotEmpty($mockParameterLocalFieldNamePrefix)})
            #set($param.testClassLocalFieldName = "${mockParameterLocalFieldNamePrefix}${StringUtils.capitalize(${param.declaredNameWithoutPrefix})}")
        #elseif (${StringUtils.isNotEmpty($parameterLocalFieldNamePrefix)})
            #set($param.testClassLocalFieldName = "${parameterLocalFieldNamePrefix}${StringUtils.capitalize(${param.declaredNameWithoutPrefix})}")
        #end
    #else
        #if(${StringUtils.isNotEmpty($dependencyMemberNamePrefix)})
            #set($param.testClassMemberName = "${dependencyMemberNamePrefix}${StringUtils.capitalize(${param.declaredNameWithoutPrefix})}")
        #end
        #if(${StringUtils.isNotEmpty($parameterLocalFieldNamePrefix)})
            #set($param.testClassLocalFieldName = "${parameterLocalFieldNamePrefix}${StringUtils.capitalize(${param.declaredNameWithoutPrefix})}")
        #end
    #end
#end

##----------------------------------------------------------------------------------------------------------------------
## useInlineMockForParam($param)
## Updates the given parameter to use an inline mock expression; e.g. mock(Foo.class) wherever the parameter is needed.
## Param: $param the Variable to update
##----------------------------------------------------------------------------------------------------------------------
#macro(useInlineMockForParam $param)
    #if($useStaticImportForInitMocks)
        #set($param.initExpression = "mock(${param.type.canonicalName}.class)")
        #set($_ = $importLinesRequired.add("import static org.mockito.Mockito.mock;"))
    #else
        #set($param.initExpression = "Mockito.mock(${param.type.canonicalName}.class)")
        #set($_ = $importLinesRequired.add("import org.mockito.Mockito;"))
    #end
    #set($param.shouldBeMocked = true)
#end

## Declare global variables set by initializeTemplateDataModel() to enable partial code-completion in the velocity
## editor bundled with the IntelliJ IDEA Ultimate Edition.
#* @vtlvariable name="dependencies" type="java.util.List" *#
#* @vtlvariable name="memberFields" type="java.util.List" *#
#* @vtlvariable name="mockMemberFields" type="java.util.List" *#
#* @vtlvariable name="nonMockMemberFields" type="java.util.List" *#
#* @vtlvariable name="sourceClassMemberNeeded" type="boolean" *#
#* @vtlvariable name="shouldUseInjectMocks" type="boolean" *#
#* @vtlvariable name="shouldSetPackageLocalFields" type="boolean" *#
#* @vtlvariable name="mocksNeeded" type="boolean" *#
#* @vtlvariable name="shouldCreateTestsForInstanceMethods" type="boolean" *#