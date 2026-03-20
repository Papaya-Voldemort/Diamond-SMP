package io.github.diamondsmp.platform.paper.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PartyService {
    private final Map<UUID, Party> partiesByMember = new HashMap<>();
    private final Map<UUID, List<PartyInvite>> invitesByTarget = new HashMap<>();

    public InviteResult invite(UUID inviterId, UUID targetId, Duration timeout) {
        if (inviterId.equals(targetId)) {
            return InviteResult.INVALID;
        }
        cleanupExpired();
        Party inviterParty = partiesByMember.get(inviterId);
        if (inviterParty != null && inviterParty.members.contains(targetId)) {
            return InviteResult.ALREADY_IN_PARTY;
        }
        if (partiesByMember.containsKey(targetId)) {
            return InviteResult.TARGET_IN_OTHER_PARTY;
        }
        List<PartyInvite> invites = invitesByTarget.computeIfAbsent(targetId, key -> new ArrayList<>());
        Instant expiresAt = Instant.now().plus(timeout);
        for (PartyInvite invite : invites) {
            if (invite.inviterId.equals(inviterId) && invite.expiresAt.isAfter(Instant.now())) {
                return InviteResult.ALREADY_PENDING;
            }
        }
        invites.add(new PartyInvite(inviterId, targetId, expiresAt));
        return InviteResult.SENT;
    }

    public Optional<PartyInvite> accept(UUID targetId, UUID inviterId) {
        cleanupExpired();
        if (partiesByMember.containsKey(targetId)) {
            return Optional.empty();
        }
        List<PartyInvite> invites = invitesByTarget.getOrDefault(targetId, List.of());
        PartyInvite accepted = null;
        for (PartyInvite invite : invites) {
            if (invite.inviterId.equals(inviterId) && invite.expiresAt.isAfter(Instant.now())) {
                accepted = invite;
            }
        }
        if (accepted == null) {
            return Optional.empty();
        }
        invites.remove(accepted);
        if (invites.isEmpty()) {
            invitesByTarget.remove(targetId);
        }
        Party party = partiesByMember.computeIfAbsent(accepted.inviterId, Party::new);
        party.members.add(targetId);
        partiesByMember.put(targetId, party);
        return Optional.of(accepted);
    }

    public Optional<PartyInvite> acceptLatest(UUID targetId) {
        cleanupExpired();
        if (partiesByMember.containsKey(targetId)) {
            return Optional.empty();
        }
        List<PartyInvite> invites = invitesByTarget.get(targetId);
        if (invites == null || invites.isEmpty()) {
            return Optional.empty();
        }
        PartyInvite invite = invites.remove(invites.size() - 1);
        if (invites.isEmpty()) {
            invitesByTarget.remove(targetId);
        }
        Party party = partiesByMember.computeIfAbsent(invite.inviterId, Party::new);
        party.members.add(targetId);
        partiesByMember.put(targetId, party);
        return Optional.of(invite);
    }

    public Optional<PartyView> partyOf(UUID playerId) {
        cleanupExpired();
        Party party = partiesByMember.get(playerId);
        if (party == null) {
            return Optional.empty();
        }
        return Optional.of(new PartyView(party.leaderId, List.copyOf(party.members)));
    }

    public AddMemberResult addMember(UUID actorId, UUID targetId) {
        cleanupExpired();
        if (actorId.equals(targetId)) {
            return AddMemberResult.INVALID;
        }
        Party actorParty = partiesByMember.get(actorId);
        if (actorParty != null && !actorParty.leaderId.equals(actorId)) {
            return AddMemberResult.NOT_LEADER;
        }
        if (actorParty != null && actorParty.members.contains(targetId)) {
            return AddMemberResult.ALREADY_IN_PARTY;
        }
        if (partiesByMember.containsKey(targetId)) {
            return AddMemberResult.TARGET_IN_OTHER_PARTY;
        }
        Party party = actorParty == null ? new Party(actorId) : actorParty;
        party.members.add(targetId);
        partiesByMember.put(actorId, party);
        partiesByMember.put(targetId, party);
        return AddMemberResult.ADDED;
    }

    public boolean removeMemberCompletely(UUID memberId) {
        cleanupExpired();
        Party party = partiesByMember.get(memberId);
        if (party == null) {
            clearInvitesFor(memberId);
            return false;
        }
        boolean leaderRemoved = party.leaderId.equals(memberId);
        party.members.remove(memberId);
        partiesByMember.remove(memberId);
        clearInvitesFor(memberId);
        if (party.members.isEmpty()) {
            return true;
        }
        if (leaderRemoved) {
            party.leaderId = party.members.iterator().next();
        }
        if (party.members.size() == 1) {
            UUID remaining = party.members.iterator().next();
            partiesByMember.remove(remaining);
        }
        return true;
    }

    public List<PartyInvite> pendingInvites(UUID targetId) {
        cleanupExpired();
        return List.copyOf(invitesByTarget.getOrDefault(targetId, List.of()));
    }

    public boolean leave(UUID playerId) {
        Party party = partiesByMember.get(playerId);
        if (party == null) {
            return false;
        }
        party.members.remove(playerId);
        partiesByMember.remove(playerId);
        if (party.members.isEmpty()) {
            return true;
        }
        if (party.leaderId.equals(playerId)) {
            UUID newLeader = party.members.iterator().next();
            party.leaderId = newLeader;
        }
        if (party.members.size() == 1) {
            UUID remaining = party.members.iterator().next();
            partiesByMember.remove(remaining);
        }
        return true;
    }

    public boolean kick(UUID actorId, UUID targetId) {
        Party party = partiesByMember.get(actorId);
        if (party == null || !party.leaderId.equals(actorId) || !party.members.contains(targetId) || actorId.equals(targetId)) {
            return false;
        }
        party.members.remove(targetId);
        partiesByMember.remove(targetId);
        if (party.members.size() == 1) {
            UUID remaining = party.members.iterator().next();
            partiesByMember.remove(remaining);
        }
        return true;
    }

    public boolean disband(UUID actorId) {
        Party party = partiesByMember.get(actorId);
        if (party == null || !party.leaderId.equals(actorId)) {
            return false;
        }
        for (UUID memberId : List.copyOf(party.members)) {
            partiesByMember.remove(memberId);
        }
        return true;
    }

    public int removeMembers(Iterable<UUID> memberIds) {
        int removed = 0;
        for (UUID memberId : memberIds) {
            if (leave(memberId)) {
                removed++;
            }
        }
        return removed;
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        invitesByTarget.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(invite -> invite.expiresAt.isBefore(now));
            return entry.getValue().isEmpty();
        });
    }

    private void clearInvitesFor(UUID memberId) {
        invitesByTarget.remove(memberId);
        invitesByTarget.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(invite -> invite.inviterId.equals(memberId) || invite.targetId.equals(memberId));
            return entry.getValue().isEmpty();
        });
    }

    public enum InviteResult {
        SENT,
        ALREADY_PENDING,
        ALREADY_IN_PARTY,
        TARGET_IN_OTHER_PARTY,
        INVALID
    }

    public enum AddMemberResult {
        ADDED,
        ALREADY_IN_PARTY,
        TARGET_IN_OTHER_PARTY,
        NOT_LEADER,
        INVALID
    }

    public record PartyInvite(UUID inviterId, UUID targetId, Instant expiresAt) {}

    public record PartyView(UUID leaderId, List<UUID> members) {
        public int size() {
            return members.size();
        }
    }

    private static final class Party {
        private UUID leaderId;
        private final LinkedHashSet<UUID> members = new LinkedHashSet<>();

        private Party(UUID leaderId) {
            this.leaderId = leaderId;
            this.members.add(leaderId);
        }
    }
}
