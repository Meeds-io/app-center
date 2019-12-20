const path = require('path');
const merge = require('webpack-merge');
const common = require('./webpack.common.js');
const webpack = require('webpack');

module.exports = merge(common, {

    devtool: 'source-map',

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
    plugins: [

        //new UglifyJSPlugin({
        //    sourceMap: true
        //}),
        new webpack.DefinePlugin({
            'process.env.NODE_ENV': JSON.stringify('production')
        })
    ]
});