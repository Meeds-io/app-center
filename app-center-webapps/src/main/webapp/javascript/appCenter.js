(function($) {
    var AppCenter = {
        init : function() {
            $(document).ready(function () {
                $(document).on('click', '.uiApplicationIcon', function (e) {
                    $('body').addClass('open-app-drawer');
                });

                $(document).on('click', '#app-center-launcher .v-overlay ,.appLauncherDrawerClose', function () {
                    $('body').removeClass('open-app-drawer');
                });
            });
        }
     };
    return AppCenter;
})($);