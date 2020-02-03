const path = require('path');
const merge = require('webpack-merge');
const common = require('./webpack.common.js');

// the display name of the war
const app = 'app-center';

// add the server path to your server location path
const exoServerPath = "/server-exo";

module.exports = merge(common, {

    output: {
        path: path.resolve(`${exoServerPath}/webapps/${app}/`)
    },
});