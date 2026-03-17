const express = require('express');
const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');
const bodyParser = require('body-parser');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(bodyParser.json());

// MongoDB Atlas connection string
const MONGODB_URI = process.env.MONGODB_URI || 'mongodb+srv://applebro47_db_user:pushkar123@cluster0.gpfpsuh.mongodb.net/rgm_db?retryWrites=true&w=majority&appName=Cluster0';

// Improved connection logic
mongoose.connect(MONGODB_URI, {
    serverSelectionTimeoutMS: 5000 // Timeout after 5s instead of 30s
}).then(() => {
    console.log("✅ Successfully connected to MongoDB Atlas");
}).catch(err => {
    console.error("❌ MongoDB connection error details:");
    console.error(err);
});

// Disable buffering so we get errors immediately if the DB is down
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

// --- Routes ---
app.get('/', (req, res) => {
    res.send('RGM Backend is running and connected to ' + (mongoose.connection.readyState === 1 ? 'DB' : 'NOTHING'));
});

app.post('/api/register', async (req, res) => {
    if (mongoose.connection.readyState !== 1) {
        return res.status(503).send({ error: "Database not connected. Please check Atlas Network Access." });
    }
    try {
        const { username, password } = req.body;
        console.log("Registering user:", username);
        const hashedPassword = await bcrypt.hash(password, 10);
        const user = new User({ username, password: hashedPassword, nickname: username });
        await user.save();
        res.status(201).send({ message: "User registered successfully" });
    } catch (error) {
        console.error("Registration error:", error);
        res.status(400).send({ error: error.message || "Registration failed" });
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
        res.status(500).send({ error: "Internal server error" });
    }
});

app.get('/api/users', async (req, res) => {
    try {
        const users = await User.find({}, { password: 0 });
        res.send(users);
    } catch (error) {
        res.status(500).send({ error: "Failed to fetch users" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => console.log(`🚀 Server running on port ${PORT}`));
