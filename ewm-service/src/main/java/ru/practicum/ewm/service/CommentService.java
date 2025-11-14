package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.dto.UpdateCommentDto;

import java.util.List;

public interface CommentService {

    CommentDto addComment(Long userId, NewCommentDto dto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto dto);

    void deleteComment(Long userId, Long commentId);

    CommentDto getComment(Long userId, Long commentId);

    List<CommentDto> getUserComments(Long userId, int from, int size);

    List<CommentDto> findComments(List<Long> users, List<Long> events,
                                  String rangeStart, String rangeEnd, int from, int size);

    CommentDto getCommentById(Long commentId);

    void deleteComment(Long commentId);

    List<CommentDto> findComments(Long eventId, List<Long> users, String rangeStart,
                                  String rangeEnd, int from, int size, HttpServletRequest request);

    CommentDto getCommentById(Long commentId, HttpServletRequest request);

    List<CommentDto> getEventComments(Long eventId, int from, int size, HttpServletRequest request);
}