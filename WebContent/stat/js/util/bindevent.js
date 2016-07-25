//绑定右键菜单事件////////////////////////
var EventUtil = new Object;
EventUtil.addEventHandler = function (oTarget, sEventType, fnHandler) {
    if (oTarget.addEventListener) { // 如果还没有绑定click事件，则进行绑定。页面载入时候会执行这里。
        oTarget.addEventListener(sEventType, fnHandler, false);
    } else if (oTarget.attachEvent) { // 查看绑定了什么事件
        oTarget.attachEvent("on" + sEventType, fnHandler);
    } else {
        oTarget["on" + sEventType] = fnHandler;
    }
};
        
EventUtil.removeEventHandler = function (oTarget, sEventType, fnHandler) {
    if (oTarget.removeEventListener) {
        oTarget.removeEventListener(sEventType, fnHandler, false);
    } else if (oTarget.detachEvent) {
        oTarget.detachEvent("on" + sEventType, fnHandler);
    } else { 
        oTarget["on" + sEventType] = null;
    }
};


// 最后起作用的函数
function handleClick() {
    //console.log("Click!");
    var insertParamDiv = document.getElementById("insertParam");
    //console.log("insertParamDiv:"+insertParamDiv);
    //console.log("newId:"+insertParamDiv.newId);
    ws.insertParameter(insertParamDiv.newId,insertParamDiv.rowIndex,insertParamDiv.type);
    // EventUtil.removeEventHandler(oDiv, "click", handleClick);
}

// 绑定DIV与触发事件，以及函数体
window.onload = function() {
    var insertParamDiv = document.getElementById("insertParam");         
    //console.log("insertParamDiv:"+insertParamDiv);
    EventUtil.addEventHandler(insertParamDiv, "click", handleClick);
};
