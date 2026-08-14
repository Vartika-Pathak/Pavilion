package com.pavilion.api.dto;

import com.pavilion.api.entity.ContactMessage;
import com.pavilion.api.entity.GalleryPhoto;
import com.pavilion.api.entity.JoinRequest;
import com.pavilion.api.entity.Member;
import com.pavilion.api.entity.NewsPost;
import com.pavilion.api.entity.ResidentMeeting;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class ContentPagesDtos {

    // ---- Gallery ----

    public record GalleryPhotoResponse(
            Long id, String imageUrl, String title, String description, String uploadedBy, Instant uploadedAt) {
        public static GalleryPhotoResponse from(GalleryPhoto photo) {
            return new GalleryPhotoResponse(
                    photo.getId(), photo.getImageUrl(), photo.getTitle(), photo.getDescription(),
                    photo.getUploadedBy(), photo.getUploadedAt());
        }
    }

    // ---- Contact ----

    public record ContactMessageResponse(
            Long id, String name, String email, String subject, String message, Instant submittedAt) {
        public static ContactMessageResponse from(ContactMessage contactMessage) {
            return new ContactMessageResponse(
                    contactMessage.getId(), contactMessage.getName(), contactMessage.getEmail(),
                    contactMessage.getSubject(), contactMessage.getMessage(), contactMessage.getSubmittedAt());
        }
    }

    public record ContactMessageRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Subject is required") String subject,
            @NotBlank(message = "Message is required") String message) {
    }

    // ---- News ----

    public record NewsPostResponse(
            Long id, String title, String content, String excerpt, String author, String imageUrl, Instant publishedAt) {
        public static NewsPostResponse from(NewsPost post) {
            return new NewsPostResponse(
                    post.getId(), post.getTitle(), post.getContent(), post.getExcerpt(), post.getAuthor(),
                    post.getImageUrl(), post.getPublishedAt());
        }
    }

    public record NewsPostRequest(
            @NotBlank(message = "Title is required") String title,
            @NotBlank(message = "Content is required") String content,
            String excerpt,
            @NotBlank(message = "Author is required") String author,
            String imageUrl) {
    }

    // ---- Resident Meetings (read-only, no admin write API existed in Node either) ----

    public record ResidentMeetingResponse(Long id, String title, Instant date, String location, String notes) {
        public static ResidentMeetingResponse from(ResidentMeeting meeting) {
            return new ResidentMeetingResponse(
                    meeting.getId(), meeting.getTitle(), meeting.getMeetingDate(), meeting.getLocation(), meeting.getNotes());
        }
    }

    // ---- Members directory (read-only, no write API existed in Node either) ----

    public record MemberResponse(Long id, String name, String flatNumber, String bio, String avatarUrl, Instant joinedAt) {
        public static MemberResponse from(Member member) {
            return new MemberResponse(
                    member.getId(), member.getName(), member.getFlatNumber(), member.getBio(),
                    member.getAvatarUrl(), member.getJoinedAt());
        }
    }

    // ---- Join Requests ----

    public record JoinRequestResponse(
            Long id, String name, String email, String flatNumber, String message, String status, Instant submittedAt) {
        public static JoinRequestResponse from(JoinRequest request) {
            return new JoinRequestResponse(
                    request.getId(), request.getName(), request.getEmail(), request.getFlatNumber(),
                    request.getMessage(), request.getStatus(), request.getSubmittedAt());
        }
    }

    public record JoinRequestRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Flat number is required") String flatNumber,
            String message) {
    }

    // ---- Community Stats ----

    public record CommunityStats(long totalMembers, long upcomingEventsCount, long totalNewsPosts, long totalGalleryPhotos) {
    }
}
