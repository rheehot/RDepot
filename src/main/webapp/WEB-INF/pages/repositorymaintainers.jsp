<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ include file="header.jsp" %>

<div id="bodyColumn">
    <c:url value="/static" var="staticUrl"/>
	<h4><spring:message code="repositorymaintainer.title"/></h4>
	<c:if test="${not empty success}">
		<div class="successblock">
		
			<spring:message code="general.success"/>
			<br />
			<spring:message code="general.info"/>
			${success}
			
		</div>
	</c:if>
	<c:if test="${not empty warning}">
		<div class="warningblock">
		
			<spring:message code="general.warning"/>
			<br />
			<spring:message code="general.info"/>
			${warning}
			
		</div>
	</c:if>
	<c:if test="${not empty error}">
		<div class="errorblock">
		
			<spring:message code="general.error"/>
			<br />
			<spring:message code="general.info"/>
			${error}
			
		</div>
	</c:if>
    
    <table id="repositorymaintainers"> 
        <tr>
            <th><spring:message code="table.header.name"/></th>
            <th><spring:message code="table.header.repository"/></th>
            <th><spring:message code="table.header.actions"/></th>
        </tr>
        <c:forEach items="${repositorymaintainers}" var="repositorymaintainer" >
            <tr id="tr${repositorymaintainer.id}">
                <td><a href="<c:url value='/manager/users' />/${repositorymaintainer.user.login}">${repositorymaintainer.user.name}</a></td>
                <td>${repositorymaintainer.repository.name} (${repositorymaintainer.repository.publicationUri})</td>
                <td><a href="<c:url value='/manager/repositories/maintainers' />/${repositorymaintainer.id}/edit"><img data-placement="bottom" data-toggle="tooltip" data-original-title="<spring:message code='table.actions.edit'/>" src="${staticUrl}/img/edit.png"></a> <a id="del${repositorymaintainer.id}" onclick="deleteRepositorymaintainer(${repositorymaintainer.id})" href="#"><img data-placement="bottom" data-toggle="tooltip" data-original-title="<spring:message code='table.actions.delete'/>" src="${staticUrl}/img/delete.png"></a></td>
            </tr>
        </c:forEach>
        <tr>
            <td colspan="3"><a class="btn btn-success" href="<c:url value='/manager/repositories/maintainers/create' />">+ <spring:message code="menu.general.create"/></a></td>
        </tr>
    </table>
    
</div>
   
<script type="text/javascript">

    var $dialog = $('<div></div>');
    $dialog.html('<spring:message code="repositorymaintainer.delete.warning"/>');
    
    /* $.ajax({
        type: "GET",
        dataType: 'json',
        url: "<c:url value="/manager/repositories/list" />",
        success: function(data)
        {
            var repositores = "";
          
            for(var i = 0, l = data.length; i < l; i++)
            {
                repositories += '<tr id="tr' + data[i].id + '"><td><a href="<c:url value="/manager/repository" />/' + data[i].id + '">' + data[i].name + '</a></td><td>' + data[i].publicationUri + '</td><td>' + data[i].serverAddress + '</td><td>' + data[i].version + '</td><td>' + data[i].packages.length + '</td><td><a id="del' + data[i].id + '" href="#"><spring:message code="table.actions.delete"/></a></td></tr>';
            }
            $(document).ready(function(){
                $("#repositories").append(repositories);       
            });
        }
    }); */

    $(document).ready(function(){
	    $dialog.dialog({ autoOpen: false, title: "<spring:message code="repositorymaintainer.delete.title"/>", dialogClass: "alert", modal: true });
    });

    function deleteRepositorymaintainer(id)
    {
        var postUrl = "<c:url value="/manager/repositories/maintainers" />" + "/" + String(id) + "/delete";
	    $dialog.dialog( "option", "buttons", { "<spring:message code="dialog.no"/>": function() { $(this).dialog("close"); }, "<spring:message code="dialog.yes"/>": function() {
	        $(this).dialog("close");
	        $.ajax({
                type: "DELETE",
                dataType: 'json',
                url: postUrl,
                success: function(data)
                {
                    if(data.success != null)
                    {
                        alert(data.success);
                        $("#tr"+String(id)).remove();
                    }
                    else
                    {
                        alert(data.error);
                    }
                }});	    
	       }});
	    $dialog.dialog("open");
    }

</script>
<%@ include file="footer.jsp" %>