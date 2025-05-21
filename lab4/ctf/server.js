const express = require("express");
const app = express();
const path = require("path");
const fs = require("fs")
var bodyParser = require("body-parser");
app.use(bodyParser.urlencoded({ extended: true }));

// this will accept all the calls to root URL http://localhost:8080/
// It will render the index.html available in the Project root directory as a Response
app.get("/", (req, res) => {
    res.sendFile(path.join(__dirname + "/index.html"));
    //__dirname : It will resolve to your project folder.
});

app.post("/result", (req, res) => {
    handle(req.body.script, res);
});

app.listen(8080);

function handle(script, res) {
    res.writeHead(200, { "Content-Type": "text/html" });
    const ans = require("./calc")(script);

    fs.readFile(path.join(__dirname, 'index.html'), 'utf8', (err, html) => {
        if (err) {
            res.writeHead(500, { "Content-Type": "text/plain" });
            res.end("Internal Server Error");
            return;
        }

        // Replace a placeholder in the HTML with the result.
        const modifiedHtml = html.replace('input value=""', `input value="${ans}"`);

        res.end(modifiedHtml);
    });
}
