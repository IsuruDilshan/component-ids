/*! modernizr 3.0.0 (Custom Build) | MIT *
 * http://modernizr.com/download/?-cssanimations-csscolumns-csstransforms-csstransforms3d-csstransitions-flexbox !*/
!function(e,n,t){function s(e,n){return typeof e===n}function r(){var e,n,t,r,o,i,a;for(var l in y){if(e=[],n=y[l],n.name&&(e.push(n.name.toLowerCase()),n.options&&n.options.aliases&&n.options.aliases.length))for(t=0;t<n.options.aliases.length;t++)e.push(n.options.aliases[t].toLowerCase());for(r=s(n.fn,"function")?n.fn():n.fn,o=0;o<e.length;o++)i=e[o],a=i.split("."),1===a.length?Modernizr[a[0]]=r:(!Modernizr[a[0]]||Modernizr[a[0]]instanceof Boolean||(Modernizr[a[0]]=new Boolean(Modernizr[a[0]])),Modernizr[a[0]][a[1]]=r),S.push((r?"":"no-")+a.join("-"))}}function o(e){var n=x.className,t=Modernizr._config.classPrefix||"";if(w&&(n=n.baseVal),Modernizr._config.enableJSClass){var s=new RegExp("(^|\\s)"+t+"no-js(\\s|$)");n=n.replace(s,"$1"+t+"js$2")}Modernizr._config.enableClasses&&(n+=" "+t+e.join(" "+t),w?x.className.baseVal=n:x.className=n)}function i(e,n){return!!~(""+e).indexOf(n)}function a(){return"function"!=typeof n.createElement?n.createElement(arguments[0]):w?n.createElementNS.call(n,"http://www.w3.org/2000/svg",arguments[0]):n.createElement.apply(n,arguments)}function l(){var e=n.body;return e||(e=a(w?"svg":"body"),e.fake=!0),e}function f(e,t,s,r){var o,i,f,u,d="modernizr",p=a("div"),c=l();if(parseInt(s,10))for(;s--;)f=a("div"),f.id=r?r[s]:d+(s+1),p.appendChild(f);return o=a("style"),o.type="text/css",o.id="s"+d,(c.fake?c:p).appendChild(o),c.appendChild(p),o.styleSheet?o.styleSheet.cssText=e:o.appendChild(n.createTextNode(e)),p.id=d,c.fake&&(c.style.background="",c.style.overflow="hidden",u=x.style.overflow,x.style.overflow="hidden",x.appendChild(c)),i=t(p,e),c.fake?(c.parentNode.removeChild(c),x.style.overflow=u,x.offsetHeight):p.parentNode.removeChild(p),!!i}function u(e){return e.replace(/([A-Z])/g,function(e,n){return"-"+n.toLowerCase()}).replace(/^ms-/,"-ms-")}function d(n,s){var r=n.length;if("CSS"in e&&"supports"in e.CSS){for(;r--;)if(e.CSS.supports(u(n[r]),s))return!0;return!1}if("CSSSupportsRule"in e){for(var o=[];r--;)o.push("("+u(n[r])+":"+s+")");return o=o.join(" or "),f("@supports ("+o+") { #modernizr { position: absolute; } }",function(e){return"absolute"==getComputedStyle(e,null).position})}return t}function p(e){return e.replace(/([a-z])-([a-z])/g,function(e,n,t){return n+t.toUpperCase()}).replace(/^-/,"")}function c(e,n,r,o){function l(){u&&(delete k.style,delete k.modElem)}if(o=s(o,"undefined")?!1:o,!s(r,"undefined")){var f=d(e,r);if(!s(f,"undefined"))return f}for(var u,c,m,v,h,g=["modernizr","tspan"];!k.style;)u=!0,k.modElem=a(g.shift()),k.style=k.modElem.style;for(m=e.length,c=0;m>c;c++)if(v=e[c],h=k.style[v],i(v,"-")&&(v=p(v)),k.style[v]!==t){if(o||s(r,"undefined"))return l(),"pfx"==n?v:!0;try{k.style[v]=r}catch(y){}if(k.style[v]!=h)return l(),"pfx"==n?v:!0}return l(),!1}function m(e,n){return function(){return e.apply(n,arguments)}}function v(e,n,t){var r;for(var o in e)if(e[o]in n)return t===!1?e[o]:(r=n[e[o]],s(r,"function")?m(r,t||n):r);return!1}function h(e,n,t,r,o){var i=e.charAt(0).toUpperCase()+e.slice(1),a=(e+" "+_.join(i+" ")+i).split(" ");return s(n,"string")||s(n,"undefined")?c(a,n,r,o):(a=(e+" "+P.join(i+" ")+i).split(" "),v(a,n,t))}function g(e,n,s){return h(e,t,t,n,s)}var y=[],C={_version:"3.0.0",_config:{classPrefix:"",enableClasses:!0,enableJSClass:!0,usePrefixes:!0},_q:[],on:function(e,n){var t=this;setTimeout(function(){n(t[e])},0)},addTest:function(e,n,t){y.push({name:e,fn:n,options:t})},addAsyncTest:function(e){y.push({name:null,fn:e})}},Modernizr=function(){};Modernizr.prototype=C,Modernizr=new Modernizr;var S=[],x=n.documentElement,w="svg"===x.nodeName.toLowerCase(),b="Moz O ms Webkit",_=C._config.usePrefixes?b.split(" "):[];C._cssomPrefixes=_;var T={elem:a("modernizr")};Modernizr._q.push(function(){delete T.elem});var k={style:T.elem.style};Modernizr._q.unshift(function(){delete k.style});var P=C._config.usePrefixes?b.toLowerCase().split(" "):[];C._domPrefixes=P,C.testAllProps=h,C.testAllProps=g,Modernizr.addTest("cssanimations",g("animationName","a",!0)),Modernizr.addTest("flexbox",g("flexBasis","1px",!0)),Modernizr.addTest("csstransforms",function(){return-1===navigator.userAgent.indexOf("Android 2.")&&g("transform","scale(1)",!0)});var z=C.testStyles=f,E="CSS"in e&&"supports"in e.CSS,N="supportsCSS"in e;Modernizr.addTest("supports",E||N),Modernizr.addTest("csstransforms3d",function(){var e=!!g("perspective","1px",!0),n=Modernizr._config.usePrefixes;if(e&&(!n||"webkitPerspective"in x.style)){var t;Modernizr.supports?t="@supports (perspective: 1px)":(t="@media (transform-3d)",n&&(t+=",(-webkit-transform-3d)")),t+="{#modernizr{left:9px;position:absolute;height:5px;margin:0;padding:0;border:0}}",z(t,function(n){e=9===n.offsetLeft&&5===n.offsetHeight})}return e}),Modernizr.addTest("csstransitions",g("transition","all",!0)),function(){Modernizr.addTest("csscolumns",function(){var e=!1,n=g("columnCount");try{(e=!!n)&&(e=new Boolean(e))}catch(t){}return e});for(var e,n,t=["Width","Span","Fill","Gap","Rule","RuleColor","RuleStyle","RuleWidth","BreakBefore","BreakAfter","BreakInside"],s=0;s<t.length;s++)e=t[s].toLowerCase(),n=g("column"+t[s]),("breakbefore"===e||"breakafter"===e||"breakinside"==e)&&(n=n||g(t[s])),Modernizr.addTest("csscolumns."+e,n)}(),r(),o(S),delete C.addTest,delete C.addAsyncTest;for(var A=0;A<Modernizr._q.length;A++)Modernizr._q[A]();e.Modernizr=Modernizr}(window,document);