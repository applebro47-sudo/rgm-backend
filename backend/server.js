const express = require('express');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const bodyParser = require('body-parser');
const cors = require('cors');
const path = require('path');

const app = express();
app.use(cors());
app.use(bodyParser.json());

// Serve static files from the current directory (index.html, SYNAPSE.apk)
app.use(express.static(__dirname));

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://applebro47_db_user:pushkar123@cluster0.gpfpsuh.mongodb.net/rgm_db?retryWrites=true&w=majority&appName=Cluster0';

mongoose.connect(MONGODB_URI, {
    serverSelectionTimeoutMS: 30000,
    connectTimeoutMS: 30000
}).then(() => {
    console.log("✅ SUCCESS: Connected to MongoDB Atlas");
}).catch(err => {
    console.error("❌ FAILURE: Could not connect to MongoDB Atlas");
    console.error("Reason:", err.message);
});

mongoose.set('bufferCommands', false);

// User Schema
const userSchema = new mongoose.Schema({
    username: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    nickname: String,
    birthday: String,
    comment: String,
    profileImage: String,
    chattedWith: [String]
});
const User = mongoose.model('User', userSchema);

// Post Schema
const postSchema = new mongoose.Schema({
    id: String,
    owner: String,
    mediaUri: String,
    mediaType: String,
    caption: String,
    likes: [String],
    comments: [{
        user: String,
        text: String,
        timestamp: Number
    }],
    timestamp: { type: Number, default: Date.now }
});
const Post = mongoose.model('Post', postSchema);

const checkDb = (req, res, next) => {
    if (mongoose.connection.readyState !== 1) {
        return res.status(503).send({ error: "Database not connected." });
    }
    next();
};

app.get('/api/status', (req, res) => {
    res.send({
        server: "online",
        db_status: mongoose.connection.readyState === 1 ? "connected" : "disconnected"
    });
});

app.get('/api/check-username/:username', checkDb, async (req, res) => {
    try {
        const user = await User.findOne({ username: req.params.username });
        res.send({ available: !user });
    } catch (error) {
        res.status(500).send({ error: "Check failed" });
    }
});

app.post('/api/register', checkDb, async (req, res) => {
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

app.post('/api/login', checkDb, async (req, res) => {
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

app.put('/api/user/:username', checkDb, async (req, res) => {
    try {
        const { nickname, birthday, comment, profileImage } = req.body;
        const user = await User.findOneAndUpdate(
            { username: req.params.username },
            { nickname, birthday, comment, profileImage },
            { new: true }
        );
        res.send(user);
    } catch (error) {
        res.status(500).send({ error: "Failed to update profile" });
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

app.get('/api/chats/:username', checkDb, async (req, res) => {
    try {
        const user = await User.findOne({ username: req.params.username });
        res.send(user ? user.chattedWith || [] : []);
    } catch (error) {
        res.status(500).send({ error: "Failed to fetch chats" });
    }
});

app.post('/api/chats/:username/:otherUser', checkDb, async (req, res) => {
    try {
        const { username, otherUser } = req.params;
        await User.updateOne({ username }, { $addToSet: { chattedWith: otherUser } });
        await User.updateOne({ username: otherUser }, { $addToSet: { chattedWith: username } });
        res.send({ message: "Chat list updated" });
    } catch (error) {
        res.status(500).send({ error: "Failed to update chat list" });
    }
});

app.get('/api/posts', checkDb, async (req, res) => {
    try {
        const posts = await Post.find({ mediaType: 'IMAGE' }).sort({ timestamp: -1 });
        res.send(posts);
    } catch (error) {
        res.status(500).send({ error: "Failed to fetch posts" });
    }
});

app.get('/api/reels', checkDb, async (req, res) => {
    try {
        const reels = await Post.find({ mediaType: 'VIDEO' }).sort({ timestamp: -1 });
        res.send(reels);
    } catch (error) {
        res.status(500).send({ error: "Failed to fetch reels" });
    }
});

app.post('/api/posts', checkDb, async (req, res) => {
    try {
        const post = new Post(req.body);
        await post.save();
        res.status(201).send(post);
    } catch (error) {
        res.status(400).send({ error: error.message });
    }
});

// Landing page route
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Server on port ${PORT}`));
