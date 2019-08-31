package com.example.websocketdemo.controller;

import com.example.websocketdemo.dao.MessageDao;
import com.example.websocketdemo.dao.RoomDao;
import com.example.websocketdemo.dao.UserDao;
import com.example.websocketdemo.entities.RoomResponse;
import com.example.websocketdemo.model.RoomModel;
import com.example.websocketdemo.model.UserModel;
import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/room")
public class RoomController {
    private RoomDao roomDao;
    private MessageDao messageDao;
    private UserDao userDao;

    public RoomController(RoomDao roomDao, MessageDao messageDao, UserDao userDao) {
        this.roomDao = roomDao;
        this.messageDao = messageDao;
        this.userDao = userDao;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public List<Responsible> index(Authentication authentication) {
        UserModel userModel = (UserModel) authentication.getCredentials();
        ObjectId id = userModel.getId();
        List<RoomModel> rooms = roomDao.getAllMemberRooms(id);
        List<Responsible> r = new ArrayList<>();
        for (RoomModel room : rooms) {
            RoomResponse _r = RoomResponse.fromModel(room);
            _r.makeLastMessage(messageDao);
            _r.makeMembers(userDao);
            _r.makeAvatar(userModel.getUsername());
            r.add(_r);
        }
        return r;
    }

    @RequestMapping(value = "/info/{id}", method = RequestMethod.GET)
    public Responsible getInfo(@PathVariable("id") ObjectId id, Authentication authentication) {
        UserModel userModel = (UserModel) authentication.getCredentials();
        ObjectId userId = userModel.getId();
        RoomModel room = roomDao.findByIdAndUserId(id, userId);
        RoomResponse result = RoomResponse.fromModel(room);
        result.makeLastMessage(messageDao);
        result.makeMembers(userDao);
        result.makeAvatar(userModel.getUsername());
        return result;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public List<Responsible> getSearch(@Max(100) @RequestParam("text") String text, Authentication authentication) {
        UserModel userModel = (UserModel) authentication.getCredentials();
        if (text.length() < 1) {
            return Collections.emptyList();
        }
        List<UserModel> userModels = userDao.searchBy(text, 3);
        return userModels.stream().map(usr -> {
            ObjectId id = usr.getId();
            RoomModel room = roomDao.findRoomWithTwoMembers(id, userModel.getId());
            if (room == null) return null;
            RoomResponse roomResponse = RoomResponse.fromModel(room);
            roomResponse.makeLastMessage(messageDao);
            roomResponse.makeMembers(userDao);
            roomResponse.makeAvatar(userModel.getUsername());
            return roomResponse;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
