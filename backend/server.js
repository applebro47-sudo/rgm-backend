const express = require('express');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const bodyParser = require('body-parser');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(bodyParser.json());

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://applebro47_db_user:pushkar123@cluster0.gpfpsuh.mongodb.net/rgm_db?retryWrites=true&w=majority&appName=Cluster0';

// Connect with improved settings for cloud environments
mongoose.connect(MONGODB_URI, {
    serverSelectionTimeoutMS: 10000, // 10s timeout
})
.then(() => console.log("✅ Database Connected Successfully"))
.catch(err => console.error("❌ Database Connection Error:", err.message));

// Disable buffering so we see errors immediately
mongoose.set('bufferCommands', false);

const User = mongoose.model('User', new mongoose.Schema({
    username: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    nickname: String
}));

app.get('/api/status', (req, res) => {
    res.send({
        server: "online",
        database: mongoose.connection.readyState === 1 ? "connected" : "disconnected"
    });
});

app.post('/api/register', async (req, res) => {
    if (mongoose.connection.readyState !== 1) {
        return res.status(503).send({ error: "Database not ready. Check Atlas IP Whitelist." });
    }
    try {
        const { username, password } = req.body;
        const hashedPassword = await bcrypt.hash(password, 10);
        const user = new User({ username, password: hashedPassword, nickname: username });
        await user.save();
        res.status(201).send({ message: "Registered" });
    } catch (error) {
        res.status(400).send({ error: error.message });
    }
});

app.post('/api/login', async (req, res) => {
    try {
        const { username, password } = req.body;
        const user = await User.findOne({ username });
        if (user && await bcrypt.compare(password, user.password)) {
            res.send(user);
        } else {
            res.status(401).send({ error: "Invalid credentials" });
        }
    } catch (error) {
        res.status(500).send({ error: "Server error" });
    }
});

app.get('/', (req, res) => res.send('RGM Backend v2 is live!'));

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Server listening on port ${PORT}`));
