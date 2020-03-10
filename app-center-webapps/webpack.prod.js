const path = require('path');
const merge = require('webpack-merge');
const common = require('./webpack.common.js');

// the display name of the war
const app = 'app-center';

module.exports = merge(common, {
    output: {
    	path: path.resolve(__dirname, `./target/${app}/`)
    },
});