const express = require('express');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const bodyParser = require('body-parser');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(bodyParser.json());

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://applebro47_db_user:pushkar123@cluster0.gpfpsuh.mongodb.net/rgm_db?retryWrites=true&w=majority&appName=Cluster0';

// 1. Connection with robust timeouts
mongoose.connect(MONGODB_URI, {
    serverSelectionTimeoutMS: 30000,
    connectTimeoutMS: 30000
}).then(() => {
    console.log("✅ SUCCESS: Connected to MongoDB Atlas");
}).catch(err => {
    console.error("❌ FAILURE: Could not connect to MongoDB Atlas");
    console.error("Reason:", err.message);
});

// 2. Disable buffering to see real errors immediately
mongoose.set('bufferCommands', false);

// --- Schemas ---

const userSchema = new mongoose.Schema({
    username: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    nickname: String,
    birthday: String,
    comment: String,
    profileImage: String
});
const User = mongoose.model('User', userSchema);

const commentSchema = new mongoose.Schema({
    id: String,
    user: String,
    text: String,
    timestamp: { type: Number, default: Date.now }
});

const postSchema = new mongoose.Schema({
    id: String,
    owner: String,
    mediaUri: String,
    mediaType: String,
    caption: String,
    likes: [String],
    comments: [commentSchema],
    timestamp: { type: Number, default: Date.now }
});
const Post = mongoose.model('Post', postSchema);

// --- Middleware: Check DB Connection ---
const checkDb = (req, res, next) => {
    if (mongoose.connection.readyState !== 1) {
        return res.status(503).send({ error: "Database not connected. Check Atlas IP Whitelist." });
    }
    next();
};

// --- Routes ---

app.get('/api/status', (req, res) => {
    res.send({
        server: "online",
        db_status: mongoose.connection.readyState === 1 ? "connected" : "disconnected"
    });
});

app.post('/api/register', checkDb, async (req, res) => {
    try {
        const { username, password } = req.body;
        console.log("Registering user:", username);
        const hashedPassword = await bcrypt.hash(password, 10);
        const user = new User({ username, password: hashedPassword, nickname: username });
        await user.save();
        res.status(201).send({ message: "Registered" });
    } catch (error) {
        console.error("Registration error:", error.message);
        res.status(400).send({ error: error.message });
    }
});

app.post('/api/login', checkDb, async (req, res) => {
    try {
        const { username, password } = req.body;
        console.log("Login attempt:", username);
        const user = await User.findOne({ username });
        if (user && await bcrypt.compare(password, user.password)) {
            res.send(user);
        } else {
            res.status(401).send({ error: "Invalid credentials" });
        }
    } catch (error) {
        console.error("Login error:", error.message);
        res.status(500).send({ error: "Server error" });
    }
});

app.get('/api/users', checkDb, async (req, res) => {
    try {
        const users = await User.find({}, { password: 0 });
        res.send(users);
    } catch (error) {
        res.status(500).send({ error: "Failed to fetch users" });
    }
});

app.put('/api/user/:username', checkDb, async (req, res) => {
    try {
        const user = await User.findOneAndUpdate(
            { username: req.params.username },
            req.body,
            { new: true }
        );
        res.send(user);
    } catch (error) {
        res.status(400).send({ error: "Update failed" });
    }
});

app.get('/api/posts', checkDb, async (req, res) => {
    try {
        const posts = await Post.find().sort({ timestamp: -1 });
        res.send(posts);
    } catch (error) {
        res.status(500).send({ error: "Failed to fetch posts" });
    }
});

app.post('/api/posts', checkDb, async (req, res) => {
    try {
        const post = new Post(req.body);
        await post.save();
        res.status(201).send(post);
    } catch (error) {
        res.status(400).send({ error: "Post creation failed" });
    }
});

app.get('/api/reels', checkDb, async (req, res) => {
    try {
        const reels = await Post.find({ mediaType: "VIDEO" }).sort({ timestamp: -1 });
        res.send(reels);
    } catch (error) {
        res.status(500).send({ error: "Failed to fetch reels" });
    }
});

app.get('/api/chats/:username', checkDb, async (req, res) => {
    try {
        const users = await User.find({ username: { $ne: req.params.username } }, { username: 1 });
        res.send(users.map(u => u.username));
    } catch (error) {
        res.status(500).send({ error: "Failed to fetch chatted users" });
    }
});

app.get('/', (req, res) => res.send('RGM Backend is Live!'));

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Server on port ${PORT}`));
