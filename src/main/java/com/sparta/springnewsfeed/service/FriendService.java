package com.sparta.springnewsfeed.service;


import com.sparta.springnewsfeed.FriendStatus;
import com.sparta.springnewsfeed.dto.*;
import com.sparta.springnewsfeed.entity.Friend;
import com.sparta.springnewsfeed.entity.User;
import com.sparta.springnewsfeed.repository.FriendRepository;
import com.sparta.springnewsfeed.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    private final FriendStatus friendWaitStatus = FriendStatus.WAITING;
    private final FriendStatus friendAcceptStatus = FriendStatus.ACCEPTED;
    private final FriendStatus friendRejectStatus = FriendStatus.REJECTED;



    @Transactional
    public void friendAddRequest(Long userId, FriendAddRequest friendAddRequest) {
        User fromUser = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User Not Found"));
        User toUser = userRepository.findByEmailAndNickname(friendAddRequest.getEmail(), friendAddRequest.getNickname());

        if(fromUser.equals(toUser)){
            log.error("FromUser and ToUser cannot be the same user");
            throw new IllegalArgumentException("FromUser and ToUser cannot be the same user");
        }

        if(friendRepository.existsByToUser(fromUser) && friendRepository.existsByFromUser(toUser)){
            log.error("Already Friend");
            throw new IllegalArgumentException("Already Friend");
        }

        if(toUser == null) {
            log.error("User Not Found");
            throw new NullPointerException("User Not Found");
        }


        Friend friend2 = friendRepository.findByFromUserAndToUser(fromUser, toUser);
        if(friend2 != null && friend2.getStatus().equals(friendAcceptStatus)){
            log.error("Friend Already");
            throw new IllegalArgumentException("Friend Already");
        }


        Friend friend = new Friend();
        friend.addRequest(fromUser, toUser, friendWaitStatus);
        friendRepository.save(friend);
    }

    public List<FriendRequestListResponse> friendRequestList(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User Not Found"));
        List<Friend> friendList = friendRepository.findAllByToUserAndStatus(user, friendWaitStatus);

        if(friendList.isEmpty()){
            log.error("Friend Request Not Found");
            throw new NoSuchElementException("Friend Request Not Found");
        }

        return friendList.stream().map(friend ->
                FriendRequestListResponse.of(friend.getFromUser().getNickname())).toList();
    }

    public List<UserSearchFriendResponse> userSearchFriend(Long userId, UserSearchFriendRequest request) {
        User fromUser = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User Not Found"));

        List<User> searchUser = userRepository.findByNickname(request.getNickname());
        if(searchUser.isEmpty()) {
            throw new NoSuchElementException("Nickname Not Found");
        }

        List<UserSearchFriendResponse> responseList = new ArrayList<>();

        for (User user : searchUser) {
            UserSearchFriendResponse response = new UserSearchFriendResponse();
            response.setNickname(user.getNickname());

            if(request.getNickname().equals(user.getNickname())){
                response.setState("본인");
            }


            Friend friend = friendRepository.findByFromUserAndToUser(fromUser, user);
            if(friend != null) {

                if(friend.getStatus().equals(friendAcceptStatus)) {
                    response.setState("친구");
                }else if(friend.getStatus().equals(friendWaitStatus)) {
                    response.setState("요청 보냄");
                }else {
                    response.setState("친구 아님");
                }
            } else {

                friend = friendRepository.findByFromUserAndToUser(user, fromUser);
                if(friend != null){
                    if(friend.getStatus().equals(friendAcceptStatus)){
                        response.setState("친구");
                    }else if(friend.getStatus().equals(friendWaitStatus)){
                        response.setState("요청 받음");
                    }else {
                        response.setState("친구 아님");
                    }
                }
            }

            responseList.add(response);
        }

        return responseList;
    }

    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        Friend friend = friendRepository.findById(friendId).orElseThrow(() -> new NoSuchElementException("Friend Not Found"));

        if(!friend.getFromUser().getId().equals(userId) && !friend.getToUser().getId().equals(userId)){
            log.error("Not Friend User");
            throw new IllegalArgumentException("Not Friend User");
        }

        if(friend.getStatus().equals(friendWaitStatus) || !friend.getStatus().equals(friendAcceptStatus)){
            log.error("Not Friend");
            throw new IllegalArgumentException("Not Friend");
        }

        friendRepository.delete(friend);
    }

    @Transactional
    public String friendAcceptanceRejection(Long userId, Long friendId, FriendRequestStatus status) {
        Friend friend = friendRepository.findById(friendId).orElseThrow(() -> new NoSuchElementException("Friend Not Found"));

        if(!friend.getToUser().getId().equals(userId)){
            log.error("Not ToUser");
            throw new IllegalArgumentException("Not ToUser");
        }

            if (status.getStatus().equals(friendAcceptStatus)) {
                friend.FriendAcceptance(status.getStatus());
                return "친구 요청 수락";
            } else if (status.getStatus().equals(friendRejectStatus)) {
                friend.FriendRejection(status.getStatus());
                friendRepository.delete(friend);
                return "친구 요청 거절";
            }

            log.error("Invalid Friend Request Status");
            throw new IllegalArgumentException("Invalid Friend Request Status");
    }
}
