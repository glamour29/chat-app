const BaseRepository = require('./BaseRepository');
const User = require('../models/User');

class UserRepository extends BaseRepository {
    constructor() {
        super(User);
    }

    async findByEmail(email) {
        return await this.findOne({ email });
    }

    async findByUsername(username) {
        return await this.findOne({ username });
    }

    async findOnlineUsers() {
        return await this.findAll({ isOnline: true });
    }

    async updateOnlineStatus(userId, isOnline) {
        return await this.update(userId, { isOnline });
    }

    // Cập nhật để hỗ trợ thay đổi số điện thoại
    async updateProfile(userId, { username, fullName, avatarUrl, phoneNumber }) {
        const updateData = {};
        if (username) updateData.username = username;
        if (fullName) updateData.fullName = fullName;
        if (avatarUrl) updateData.avatarUrl = avatarUrl;
        if (phoneNumber) updateData.phoneNumber = phoneNumber; // Thêm dòng này
        
        return await this.update(userId, updateData);
    }

    // Sửa hàm này để App có thể tìm kiếm bằng số điện thoại
    async searchUsers(searchTerm) {
        return await this.findAll({
            $or: [
                { username: { $regex: searchTerm, $options: 'i' } },
                { fullName: { $regex: searchTerm, $options: 'i' } },
                { phoneNumber: { $regex: searchTerm, $options: 'i' } } // Thêm tìm kiếm theo SĐT
            ]
        });
    }
    

    async getUsersExcept(excludeUserId) {
        return await this.findAll(
            { _id: { $ne: excludeUserId } },
            { select: '-password' }
        );
    }
}

module.exports = new UserRepository();