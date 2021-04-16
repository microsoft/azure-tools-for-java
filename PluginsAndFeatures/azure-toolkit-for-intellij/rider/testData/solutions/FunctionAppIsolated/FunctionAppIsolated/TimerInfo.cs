// The current templates in Azure Functions with isolated worker are broken.
// Until a new version comes which has this support, this file will need to exist.
// (anything > 4.0.1 of the Microsoft.Azure.Functions.Worker.Extensions.Timer package should work)

using System;

namespace Microsoft.Azure.Functions.Worker
{
    /// <summary>
    /// Represents a timer schedule status.
    /// </summary>
    public class ScheduleStatus
    {
        /// <summary>
        /// Gets or sets the last recorded schedule occurrence.
        /// </summary>
        public DateTime Last { get; set; }

        /// <summary>
        /// Gets or sets the expected next schedule occurrence.
        /// </summary>
        public DateTime Next { get; set; }

        /// <summary>
        /// Gets or sets the last time this record was updated. This is used to re-calculate Next
        /// with the current Schedule after a host restart.
        /// </summary>
        public DateTime LastUpdated { get; set; }
    }

    /// <summary>
    /// Provides access to timer schedule information for jobs triggered
    /// by <see cref="TimerTriggerAttribute"/>.
    /// </summary>
    public class TimerInfo
    {
        /// <summary>
        /// Gets the current schedule status for this timer.
        /// If schedule monitoring is not enabled for this timer (see <see cref="TimerTriggerAttribute.UseMonitor"/>)
        /// this property will return null.
        /// </summary>
        public ScheduleStatus? ScheduleStatus { get; set; }

        /// <summary>
        /// Gets a value indicating whether this timer invocation
        /// is due to a missed schedule occurrence.
        /// </summary>
        public bool IsPastDue { get; set; }
    }
}