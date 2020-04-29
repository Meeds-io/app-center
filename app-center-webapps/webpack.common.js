const path = require('path');
const { VueLoaderPlugin } = require('vue-loader')
const ExtractTextWebpackPlugin = require('extract-text-webpack-plugin');

module.exports = {
    context: path.resolve(__dirname, '.'),
    // set the entry point of the application
    // can use multiple entry

    entry: {
        adminSetup: "./src/main/webapp/vue-apps/adminSetup/adminSetup.js",
        userSetup: "./src/main/webapp/vue-apps/userSetup/userSetup.js",
        myApplications: "./src/main/webapp/vue-apps/myApplications/myApplications.js",
        appLauncher: "./src/main/webapp/vue-apps/appLauncher/appLauncher.js"
    },
    output: {
        filename: 'javascript/vue/[name].bundle.js',
        libraryTarget: 'amd'
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: [
                    'babel-loader'
                ]
            },
            {
                test: /\.vue$/,
                use: [
                    'vue-loader'
                ]
            },
            {
                test: /\.less$/,
                use: ExtractTextWebpackPlugin.extract({
                    fallback: 'vue-style-loader',
                    use: [
                        {
                            loader: 'css-loader',
                            options: {
                                sourceMap: true
                            }
                        },
                        {
                            loader: 'less-loader',
                            options: {
                                sourceMap: true
                            }
                        }
                    ]
                })
            }
        ]
    },
    externals: {
        vue: 'Vue',
        vuetify: 'Vuetify',
    },
    plugins: [
        // we use ExtractTextWebpackPlugin to extract the css code on a css file
        new ExtractTextWebpackPlugin('skin/css/app-center.css'),
        // make sure to include the plugin for the magic
        new VueLoaderPlugin()
    ],
};