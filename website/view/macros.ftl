<#macro resultset2json resultset>
[
    <#list resultset as qs>
    {
        <#list qs.varNames() as var>
            "${var?js_string}": "${qs.get(var)?js_string}"<#if !(var?is_last)>,</#if>
        </#list>
    }
    <#if !(qs?is_last)>,</#if>
    </#list>
]
</#macro>
