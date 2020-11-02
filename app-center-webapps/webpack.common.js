/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
const path = require('path');
const { VueLoaderPlugin } = require('vue-loader')

module.exports = {
    context: path.resolve(__dirname, '.'),
    // set the entry point of the application
    // can use multiple entry

    entry: {
        adminSetup: "./src/main/webapp/vue-apps/adminSetup/main.js",
        userSetup: "./src/main/webapp/vue-apps/userSetup/main.js",
        myApplications: "./src/main/webapp/vue-apps/myApplications/main.js",
        appLauncher: "./src/main/webapp/vue-apps/appLauncher/main.js",
        applicationSearch: "./src/main/webapp/vue-apps/application-search/main.js"
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
                    'babel-loader',
          	    'eslint-loader',
                ]
            },
            {
                test: /\.vue$/,
                use: [
                    'vue-loader',
                    'eslint-loader',
                ]
            }
        ]
    },
    externals: {
        vue: 'Vue',
        vuetify: 'Vuetify',
    },
    plugins: [
        // make sure to include the plugin for the magic
        new VueLoaderPlugin()
    ],
};
