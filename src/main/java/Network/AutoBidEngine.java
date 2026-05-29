package Network;

import Exception.AuctionClosedException;
import Exception.InsufficientBalanceException;
import Exception.InvalidBidException;
import Exception.UnauthorizedException;
import Model.AuctionSession;
import Model.AutoBid;
import DAO.AuctionDAO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Xử lý logic AutoBid: cài đặt, gỡ bỏ, second-price algorithm. Tách khỏi AuctionServer.ClientHandler. */
public class AutoBidEngine {

    private final Map<String, List<AutoBid>> autoBids = new ConcurrentHashMap<>();
    private final Object autoBidLock = new Object();
    private final AuctionDAO auctionDAO;
    private static final double DEFAULT_INCREMENT = 1.0;

    public AutoBidEngine(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    /** Cài đặt AutoBid cho user trong một phiên. */
    public void setAutoBid(String userId, String auctionId, double maxAmount, double increment) {
        synchronized (autoBidLock) {
            List<AutoBid> bids = autoBids.computeIfAbsent(auctionId, k -> new ArrayList<>());
            bids.removeIf(ab -> ab.getUserId().equals(userId));
            bids.add(new AutoBid(userId, auctionId, maxAmount, increment));
        }
        processAutoBids(auctionId);
    }

    /** Gỡ AutoBid của user khỏi phiên. */
    public void removeAutoBid(String userId, String auctionId) {
        synchronized (autoBidLock) {
            List<AutoBid> bids = autoBids.get(auctionId);
            if (bids != null) {
                bids.removeIf(ab -> ab.getUserId().equals(userId));
            }
        }
    }

    /** Xử lý AutoBid theo second-price logic, gọi 1 lần duy nhất sau khi có bid mới. */
    public void processAutoBids(String auctionId) {
        Optional<AuctionSession> auctionOpt = auctionDAO.findAuctionById(auctionId);
        if (auctionOpt.isEmpty() || !auctionOpt.get().isRunning()) {
            return;
        }

        AuctionSession auction = auctionOpt.get();
        double currentPrice = auction.getCurrentPrice();
        String highestBidderId = auction.getHighestBidderId();

        List<AutoBid> candidates;
        synchronized (autoBidLock) {
            List<AutoBid> bids = autoBids.get(auctionId);
            if (bids == null || bids.isEmpty()) return;
            candidates = new ArrayList<>(bids);
        }

        candidates.sort(Comparator.comparingDouble(AutoBid::getMaxAmount).reversed());

        AutoBid best = null;
        for (AutoBid ab : candidates) {
            boolean notWinner = !ab.getUserId().equals(highestBidderId);
            boolean canOutbid = ab.getMaxAmount() > currentPrice;
            if (notWinner && canOutbid) {
                best = ab;
                break;
            }
        }
        if (best == null) return;

        double secondMax = 0;
        for (AutoBid ab : candidates) {
            if (!ab.getUserId().equals(best.getUserId())) {
                secondMax = Math.max(secondMax, ab.getMaxAmount());
            }
        }

        double increment = best.getIncrement();
        double bidAmount;

        if (secondMax > 0) {
            bidAmount = Math.min(best.getMaxAmount(), secondMax + increment);
        } else {
            bidAmount = currentPrice + increment;
        }

        if (bidAmount <= currentPrice) {
            bidAmount = Math.min(currentPrice + increment, best.getMaxAmount());
        }
        if (bidAmount <= currentPrice || bidAmount > best.getMaxAmount()) {
            return;
        }

        try {
            auctionDAO.placeBid(auctionId, best.getUserId(), bidAmount);
        } catch (AuctionClosedException | InvalidBidException | UnauthorizedException e) {
            System.err.println("[AutoBid] Error: " + e.getMessage());
        } catch (InsufficientBalanceException e) {
            synchronized (autoBidLock) {
                List<AutoBid> bids = autoBids.get(auctionId);
                if (bids != null) bids.remove(best);
            }
        }
    }
}
