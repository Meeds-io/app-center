const path = require('path');
const merge = require('webpack-merge');
const common = require('./webpack.common.js');

module.exports = merge(common, {

    context: path.resolve(__dirname, 'vue-apps'),

    entry: {
    	adminSetup: "./adminSetup/adminSetup.js",
    	userSetup: "./userSetup/userSetup.js",
    	myApplications: "./myApplications/myApplications.js"
    },
    output: {
    	path: path.resolve(__dirname, '../webapp/javascript/vue/'),
    	filename: '[name].bundle.js',
    	libraryTarget: 'amd'
    },

    devtool: 'inline-source-map',
});