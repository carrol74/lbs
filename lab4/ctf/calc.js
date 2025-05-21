// const vm = require("vm");

// function evalInJail(env, options, script) {
//     const context = vm.createContext(env);
//     const src = `"use strict";\n${script}`;

//     return vm.createScript(src).runInNewContext(context, options);
// }

// function main(line) {
//     const env = {};

//     const options = {
//         timeout: 1000,
//     };

//     const blacklist = [
//         "abort",
//         "kill",
//         "exit",
//         "Error",
//         "throw",
//         "Promise",
//         "emit",
//         "quit",
//     ];

//     var res = "";
//     try {
//         for (item of blacklist) {
//             if (line.toLowerCase().includes(item)) {
//                 throw "devil attempts!";
//             }
//         }
//         res = evalInJail(env, options, line);
//     } catch (e) {
//         res = "invalid input!";
//     }

//     return res;
// }

// process.on("uncaughtException", function(err) {
//     console.log("...");
// });

// module.exports = main;

const { Parser } = require("expr-eval");

function main(line) {
    let result;
    try {
        const parser = new Parser();
        result = parser.evaluate(line);
    } catch (e) {
        result = "invalid input!";
    }
    return result;
}

module.exports = main;
