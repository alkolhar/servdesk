package dev.alkolhar.servdesk.ticket;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * No self link is added: unlike every other resource in this API, an individual
 * {@link TicketComment} has no {@code findById}-style endpoint of its own —
 * {@code CommentController} only supports create + list on the
 * {@code /api/tickets/{ticketId}/comments} collection.
 */
@Component
public class CommentModelAssembler implements RepresentationModelAssembler<TicketComment, CommentModel> {

	@Override
	public CommentModel toModel(TicketComment comment) {
		CommentModel model = new CommentModel();
		model.setId(comment.getId());
		model.setTicketId(comment.getTicket().getId());
		model.setAuthorId(comment.getAuthor().getId());
		model.setBody(comment.getBody());
		model.setInternal(comment.isInternal());
		model.setCreatedAt(comment.getCreatedAt());
		model.setUpdatedAt(comment.getUpdatedAt());
		model.setCreatedBy(comment.getCreatedBy());
		model.setUpdatedBy(comment.getUpdatedBy());
		return model;
	}
}
