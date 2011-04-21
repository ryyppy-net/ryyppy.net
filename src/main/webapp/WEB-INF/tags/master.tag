<%@tag description="Master page" pageEncoding="UTF-8"%>
<%@attribute name="customHead" fragment="true" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>Ryyppy.net</title>
        <link rel="stylesheet" type="text/css" href="/static/css/style.css" />
        <script type="text/javascript" src="/static/js/jquery.js"></script>

        <script type="text/javascript">
            var _gaq = _gaq || [];
            _gaq.push(['_setAccount', 'UA-22483744-1']);
            _gaq.push(['_trackPageview']);

            (function() {
                var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
                ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
                var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
            })();
        </script>
        <jsp:invoke fragment="customHead" />
    </head>
    <body>
        <jsp:doBody/>
    </body>
</html>